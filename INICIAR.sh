#!/bin/sh
set -eu
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
    printf '%s\n' 'Java nao encontrado. Instale Java 17 ou mais recente com suporte a interface grafica.' >&2
    exit 1
fi
exec java -jar papelaria.jar
