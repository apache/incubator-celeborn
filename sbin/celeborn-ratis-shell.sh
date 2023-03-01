#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

function printUsage {
  echo "Usage: Celeborn ratis COMMAND [GENERIC_COMMAND_OPTIONS] [COMMAND_ARGS]"
  echo
  echo "COMMAND is one of:"
  echo -e "  sh    \t Command line tool for Celeborn ratis"
  echo
  echo "GENERIC_COMMAND_OPTIONS supports:"
  echo -e "  -D<property=value>\t Use a value for a given ratis-shell property"
  echo
  echo "Commands print help when invoked without parameters."
}

function runJavaClass {
  CLASS_ARGS=()
  for arg in "$@"; do
      case "${arg}" in
          -D* | -X* | -agentlib* | -javaagent*)
              CELEBORN_RATIS_SHELL_JAVA_OPTS+=" ${arg}" ;;
          *)
              CLASS_ARGS+=("${arg}")
      esac
  done
  "${JAVA}" -cp "${CLASSPATH}" ${CELEBORN_RATIS_SHELL_JAVA_OPTS} "${CLASS}" ${PARAMETER} "${CLASS_ARGS[@]}"
}

function main {
  SBIN=$(cd "$( dirname "$( readlink "$0" || echo "$0" )" )" || exit; pwd)

  if [[ $# == 0 ]]; then
    printUsage
    exit 1
  fi

  COMMAND=$1
  shift

  . "${SBIN}"/celeborn-ratis-shell-config.sh

  PARAMETER=""

  case ${COMMAND} in
  "sh")
    CLASS="org.apache.ratis.shell.cli.sh.RatisShell"
    CLASSPATH=${CELEBORN_RATIS_SHELL_CLIENT_CLASSPATH}
    runJavaClass "$@"
  ;;
  *)
    echo "Unsupported command ${COMMAND}" >&2
    printUsage
    exit 1
  ;;
  esac
}

# Load Celeborn related env
if [ -z "${CELEBORN_HOME}" ]; then
  export CELEBORN_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

. "${CELEBORN_HOME}/sbin/celeborn-config.sh"

if [ "$CELEBORN_MASTER_MEMORY" = "" ]; then
  CELEBORN_MASTER_MEMORY="1g"
fi

main "$@"
