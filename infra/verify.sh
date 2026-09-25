#!/bin/sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
terraform_image="hashicorp/terraform:1.13.3"

run_terraform() {
    working_directory=$1
    shift
    if command -v terraform >/dev/null 2>&1; then
        terraform -chdir="$project_root/$working_directory" "$@"
        return
    fi
    docker run --rm \
        -v "$project_root:/workspace" \
        -w "/workspace/$working_directory" \
        "$terraform_image" "$@"
}

run_terraform infra fmt -check -recursive
run_terraform infra/contracts init -backend=false -input=false
run_terraform infra/contracts validate
run_terraform infra/contracts test
run_terraform infra/bootstrap init -backend=false -input=false
run_terraform infra/bootstrap validate
run_terraform infra/environments init -backend=false -input=false
run_terraform infra/environments validate
run_terraform infra/environments test
