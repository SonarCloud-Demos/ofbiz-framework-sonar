#!/usr/bin/env python3
"""Generate deterministic Phase 0 architecture inventories from OFBiz metadata."""

from __future__ import annotations

import argparse
import csv
import json
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
import xml.etree.ElementTree as ET


INTEGRATION_TERMS = (
    "authorize", "cybersource", "dhl", "email", "fedex", "ftp", "http",
    "ldap", "mail", "paypal", "rmi", "soap", "ups", "usps", "worldpay",
)
SOURCE_ROOTS = ("framework", "applications", "themes")


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def parse_xml(path: Path) -> ET.Element | None:
    try:
        return ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return None


@dataclass(frozen=True)
class Ownership:
    context: str
    owner_role: str
    status: str = "provisional-repository-inferred"


class ContextResolver:
    def __init__(self, config: dict[str, object]) -> None:
        self.default_context = str(config["default_context"])
        self.default_owner = str(config["default_owner_role"])
        raw_rules = config["rules"]
        if not isinstance(raw_rules, list):
            raise ValueError("context-map rules must be a list")
        self.rules = sorted(raw_rules, key=lambda item: len(str(item["prefix"])), reverse=True)

    def resolve(self, relative_path: str) -> Ownership:
        for rule in self.rules:
            if relative_path.startswith(str(rule["prefix"])):
                return Ownership(str(rule["context"]), str(rule["owner_role"]))
        return Ownership(self.default_context, self.default_owner)


class InventoryGenerator:
    def __init__(self, root: Path, resolver: ContextResolver) -> None:
        self.root = root
        self.resolver = resolver
        self.parse_failures: list[str] = []
        self.webapps: list[dict[str, str]] = []

    def relative(self, path: Path) -> str:
        return path.relative_to(self.root).as_posix()

    def xml_root(self, path: Path) -> ET.Element | None:
        root = parse_xml(path)
        if root is None:
            self.parse_failures.append(self.relative(path))
        return root

    def ownership_fields(self, relative_path: str) -> dict[str, str]:
        ownership = self.resolver.resolve(relative_path)
        return {
            "target_context": ownership.context,
            "owner_role": ownership.owner_role,
            "owner_status": ownership.status,
        }

    def source_files(self, pattern: str) -> list[Path]:
        files: list[Path] = []
        for source_root in SOURCE_ROOTS:
            directory = self.root / source_root
            if directory.is_dir():
                files.extend(directory.glob(pattern))
        return sorted(files)

    def components(self) -> list[dict[str, str]]:
        rows: list[dict[str, str]] = []
        for path in self.source_files("**/ofbiz-component.xml"):
            if "/src/test/" in f"/{self.relative(path)}/":
                continue
            root = self.xml_root(path)
            if root is None or local_name(root.tag) != "ofbiz-component":
                continue
            relative = self.relative(path)
            component = root.get("name", path.parent.name)
            containers = [node.get("name", "") for node in root if local_name(node.tag) == "container"]
            webapps = [node for node in root if local_name(node.tag) == "webapp"]
            resources = [node for node in root if local_name(node.tag).endswith("-resource")]
            rows.append({
                "component": component,
                "path": relative,
                "containers": ";".join(filter(None, containers)),
                "webapp_count": str(len(webapps)),
                "resource_count": str(len(resources)),
                **self.ownership_fields(relative),
            })
            for webapp in webapps:
                self.webapps.append({
                    "component": component,
                    "name": webapp.get("name", ""),
                    "mount_point": webapp.get("mount-point", ""),
                    "location": (path.parent / webapp.get("location", "")).resolve().as_posix(),
                    "source_file": relative,
                    "base_permission": webapp.get("base-permission", ""),
                    **self.ownership_fields(relative),
                })
        return rows

    def routes(self) -> list[dict[str, str]]:
        rows: list[dict[str, str]] = []
        for path in self.source_files("**/*controller.xml"):
            root = self.xml_root(path)
            if root is None:
                continue
            relative = self.relative(path)
            mount = self._mount_for(path)
            for node in root.iter():
                if local_name(node.tag) == "request-map":
                    rows.append(self._route_row(node, mount, relative))
        return rows

    def _route_row(self, node: ET.Element, mount: str, relative: str) -> dict[str, str]:
        uri = node.get("uri", "")
        security = next((item for item in node if local_name(item.tag) == "security"), None)
        event = next((item for item in node if local_name(item.tag) == "event"), None)
        responses = [item for item in node if local_name(item.tag) == "response"]
        return {
            "route": f"{mount}/control/{uri}" if mount else uri,
            "mount_point": mount,
            "request_uri": uri,
            "https": security.get("https", "") if security is not None else "",
            "auth": security.get("auth", "") if security is not None else "",
            "event_type": event.get("type", "") if event is not None else "",
            "event_target": self._event_target(event),
            "responses": ";".join(
                f"{item.get('name', '')}:{item.get('type', '')}:{item.get('value', '')}"
                for item in responses
            ),
            "source_file": relative,
            **self.ownership_fields(relative),
        }

    def _mount_for(self, controller: Path) -> str:
        controller_text = controller.resolve().as_posix()
        matches = [item for item in self.webapps if controller_text.startswith(item["location"] + "/")]
        if not matches:
            return ""
        return max(matches, key=lambda item: len(item["location"]))["mount_point"]

    @staticmethod
    def _event_target(event: ET.Element | None) -> str:
        if event is None:
            return ""
        return ":".join(filter(None, (event.get("path", ""), event.get("invoke", ""))))

    def services(self) -> list[dict[str, str]]:
        rows: list[dict[str, str]] = []
        for path in self.source_files("**/servicedef/*.xml"):
            if not path.name.startswith("services"):
                continue
            root = self.xml_root(path)
            if root is None:
                continue
            relative = self.relative(path)
            for node in root.iter():
                if local_name(node.tag) != "service":
                    continue
                name = node.get("name") or f"{node.get('verb', '')}{node.get('noun', '')}"
                rows.append({
                    "service": name,
                    "engine": node.get("engine", ""),
                    "location": node.get("location", ""),
                    "invoke": node.get("invoke", ""),
                    "default_entity": node.get("default-entity-name", ""),
                    "auth": node.get("auth", ""),
                    "export": node.get("export", ""),
                    "source_file": relative,
                    **self.ownership_fields(relative),
                })
        return rows

    def entities(self) -> list[dict[str, str]]:
        rows: list[dict[str, str]] = []
        for path in self.source_files("**/entitydef/*.xml"):
            root = self.xml_root(path)
            if root is None:
                continue
            relative = self.relative(path)
            for node in root.iter():
                kind = local_name(node.tag)
                if kind not in {"entity", "view-entity"}:
                    continue
                rows.append({
                    "entity": node.get("entity-name", ""),
                    "kind": kind,
                    "package": node.get("package-name", ""),
                    "title": node.get("title", ""),
                    "source_file": relative,
                    **self.ownership_fields(relative),
                })
        return rows

    def ecas(self) -> list[dict[str, str]]:
        rows: list[dict[str, str]] = []
        candidates = [path for path in self.source_files("**/*.xml") if "eca" in path.name.lower()]
        for path in candidates:
            root = self.xml_root(path)
            if root is None:
                continue
            relative = self.relative(path)
            for node in root.iter():
                if local_name(node.tag) != "eca":
                    continue
                source = node.get("service") or node.get("entity") or node.get("entity-name", "")
                actions = [
                    item.get("service") or item.get("name", "")
                    for item in node
                    if local_name(item.tag) == "action"
                ]
                rows.append({
                    "eca_type": "service" if node.get("service") else "entity",
                    "source": source,
                    "event": node.get("event") or node.get("operation", ""),
                    "actions": ";".join(filter(None, actions)),
                    "condition_count": str(sum(local_name(item.tag).startswith("condition") for item in node)),
                    "source_file": relative,
                    **self.ownership_fields(relative),
                })
        return rows

    def scheduled_jobs(self) -> list[dict[str, str]]:
        rows: list[dict[str, str]] = []
        for path in self.source_files("**/data/*.xml"):
            root = self.xml_root(path)
            if root is None:
                continue
            relative = self.relative(path)
            for node in root.iter():
                if local_name(node.tag) != "JobSandbox":
                    continue
                rows.append({
                    "job_id": node.get("jobId", ""),
                    "job_name": node.get("jobName", ""),
                    "service": node.get("serviceName", ""),
                    "run_time": node.get("runTime", ""),
                    "schedule_expression": node.get("tempExprId", ""),
                    "pool": node.get("poolId", ""),
                    "run_as": node.get("runAsUser", ""),
                    "source_file": relative,
                    **self.ownership_fields(relative),
                })
        return rows

    def integrations(self, services: list[dict[str, str]]) -> list[dict[str, str]]:
        rows = [{
            "integration": item["name"],
            "direction": "inbound-web",
            "mechanism": "servlet-webapp",
            "evidence": item["mount_point"],
            "source_file": item["source_file"],
            "target_context": item["target_context"],
            "owner_role": item["owner_role"],
            "owner_status": item["owner_status"],
        } for item in self.webapps]
        for service in services:
            evidence = " ".join((service["service"], service["engine"], service["location"], service["invoke"])).lower()
            terms = sorted(term for term in INTEGRATION_TERMS if term in evidence)
            if not terms:
                continue
            rows.append({
                "integration": service["service"],
                "direction": "outbound-or-adapter-review-required",
                "mechanism": ";".join(terms),
                "evidence": ":".join(filter(None, (service["engine"], service["location"], service["invoke"]))),
                "source_file": service["source_file"],
                "target_context": service["target_context"],
                "owner_role": service["owner_role"],
                "owner_status": service["owner_status"],
            })
        return rows

    def generate(self) -> dict[str, list[dict[str, str]]]:
        components = self.components()
        services = self.services()
        return {
            "components": components,
            "webapps": sorted(self.webapps, key=lambda item: (item["source_file"], item["name"])),
            "routes": self.routes(),
            "services": services,
            "entities": self.entities(),
            "ecas": self.ecas(),
            "scheduled-jobs": self.scheduled_jobs(),
            "integrations": self.integrations(services),
        }


def write_csv(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("", encoding="utf-8")
        return
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]), lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def build_summary(inventories: dict[str, list[dict[str, str]]], failures: list[str]) -> dict[str, object]:
    contexts: Counter[str] = Counter()
    for rows in inventories.values():
        contexts.update(row["target_context"] for row in rows if "target_context" in row)
    return {
        "counts": {name: len(rows) for name, rows in inventories.items()},
        "context_record_counts": dict(sorted(contexts.items())),
        "parse_failures": sorted(set(failures)),
        "ownership_status": "provisional-repository-inferred; stakeholder validation required",
        "production_metrics_status": "not derivable from source; telemetry baseline required",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--output", type=Path, default=None)
    parser.add_argument("--config", type=Path, default=Path(__file__).with_name("context-map.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    output = (args.output or root / "docs/architecture/inventory").resolve()
    config = json.loads(args.config.read_text(encoding="utf-8"))
    generator = InventoryGenerator(root, ContextResolver(config))
    inventories = generator.generate()
    for name, rows in inventories.items():
        write_csv(output / f"{name}.csv", rows)
    summary = build_summary(inventories, generator.parse_failures)
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
