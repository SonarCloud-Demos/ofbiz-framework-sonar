"""Tests for the Phase 0 inventory generator."""

from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("generate_inventory.py")
SPEC = importlib.util.spec_from_file_location("generate_inventory", MODULE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("Unable to load inventory generator")
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class InventoryGeneratorTest(unittest.TestCase):
    def test_extracts_metadata_and_assigns_most_specific_context(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            component = root / "applications/order"
            controller = component / "webapp/orders/WEB-INF"
            service_dir = component / "servicedef"
            entity_dir = component / "entitydef"
            data_dir = component / "data"
            for directory in (controller, service_dir, entity_dir, data_dir):
                directory.mkdir(parents=True, exist_ok=True)
            (component / "ofbiz-component.xml").write_text(
                '<ofbiz-component name="order"><webapp name="orders" location="webapp/orders" '
                'mount-point="/orders"/></ofbiz-component>', encoding="utf-8")
            (controller / "controller.xml").write_text(
                '<site-conf><request-map uri="find"><security https="true" auth="true"/>'
                '<event type="service" invoke="findOrders"/></request-map></site-conf>', encoding="utf-8")
            (service_dir / "services.xml").write_text(
                '<services><service name="findOrders" engine="java" auth="true"/></services>', encoding="utf-8")
            (entity_dir / "entitymodel.xml").write_text(
                '<entitymodel><entity entity-name="OrderHeader"/></entitymodel>', encoding="utf-8")
            (service_dir / "secas.xml").write_text(
                '<service-eca><eca service="findOrders" event="return"><action service="auditOrder"/>'
                '</eca></service-eca>', encoding="utf-8")
            (data_dir / "Scheduled.xml").write_text(
                '<entity-engine-xml><JobSandbox jobId="1" jobName="Order job" serviceName="findOrders"/>'
                '</entity-engine-xml>', encoding="utf-8")
            resolver = MODULE.ContextResolver({
                "default_context": "unknown",
                "default_owner_role": "unknown-owner",
                "rules": [
                    {"prefix": "applications", "context": "broad", "owner_role": "broad-owner"},
                    {"prefix": "applications/order", "context": "orders", "owner_role": "order-owner"},
                ],
            })
            inventories = MODULE.InventoryGenerator(root, resolver).generate()
            self.assertEqual("/orders/control/find", inventories["routes"][0]["route"])
            self.assertEqual("orders", inventories["services"][0]["target_context"])
            self.assertEqual("OrderHeader", inventories["entities"][0]["entity"])
            self.assertEqual("auditOrder", inventories["ecas"][0]["actions"])
            self.assertEqual("findOrders", inventories["scheduled-jobs"][0]["service"])

    def test_summary_is_deterministic_and_reports_unknown_metrics(self) -> None:
        summary = MODULE.build_summary({
            "services": [{"target_context": "orders"}],
            "routes": [{"target_context": "orders"}],
        }, ["broken.xml", "broken.xml"])
        encoded = json.dumps(summary, sort_keys=True)
        self.assertIn('"orders": 2', encoded)
        self.assertEqual(["broken.xml"], summary["parse_failures"])
        self.assertIn("not derivable", summary["production_metrics_status"])


if __name__ == "__main__":
    unittest.main()
