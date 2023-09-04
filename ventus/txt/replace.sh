#!/usr/bin/bash
cat $1 | sed -e "/\/\*/d; s/\ /\n/g" | sed "/^\@/d"