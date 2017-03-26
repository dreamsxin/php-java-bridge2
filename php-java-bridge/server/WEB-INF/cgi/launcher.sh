#!/bin/sh
# php fcgi launcher
set -x

strace -s 1024 -ff "$@" 1>&2 &
trap "kill $! && exit 0;" 1 2 15
read result 1>&2
kill $!
