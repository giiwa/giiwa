#!/bin/bash

sudo -u postgres pg_dump $1 > $2
