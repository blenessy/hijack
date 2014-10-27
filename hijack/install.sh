#!/bin/bash

DIR="${0%/*}"

tup upd
tos-bsl --swap-reset-test --invert-reset --invert-test -r -e -I -p "$DIR/out.hex"
