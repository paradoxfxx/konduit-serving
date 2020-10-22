#
# ******************************************************************************
# Copyright (c) 2020 Konduit K.K.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
# *****************************************************************************
#

# Uncomment to use the environment variables

## Environment variable name for storing port number for the konduit server.
## This variable will be prioritizes over the port set through the configuration files.
# -------------------------------------------------------------------------------------
# export KONDUIT_SERVING_PORT=8080
# -------------------------------------------------------------------------------------

## An environment variable for setting the working directory for konduit serving.
## The working directory contains the runtime files generated by vertx or konduit-serving itself.
## The runtime files could contain logs, running process details, vertx cache files etc.
# -------------------------------------------------------------------------------------
# export KONDUIT_WORKING_DIR=${HOME}/.konduit-serving
# -------------------------------------------------------------------------------------

## Environment variable specifying vertx runtime and cache directory.
# -------------------------------------------------------------------------------------
# export KONDUIT_VERTX_DIR=${KONDUIT_WORKING_DIR}/vertx
# -------------------------------------------------------------------------------------

## Environment variable specifying build data directory where build logs for the build CLI are kept.
# -------------------------------------------------------------------------------------
# export KONDUIT_BUILD_DIR=${KONDUIT_WORKING_DIR}/build
# -------------------------------------------------------------------------------------

## Environment variable specifying profiles data directory where details of individual profiles are kept.
# -------------------------------------------------------------------------------------
# export KONDUIT_PROFILES_DIR=${KONDUIT_WORKING_DIR}/profiles
# -------------------------------------------------------------------------------------

## This variable is responsible for setting the path where the log files for a konduit server
## is kept for the `/logs` endpoint.
# -------------------------------------------------------------------------------------
# export KONDUIT_ENDPOINT_LOGS_DIR=${KONDUIT_WORKING_DIR}/endpoint_logs
# -------------------------------------------------------------------------------------

## Default directory for containing the command line logs for konduit-serving
# -------------------------------------------------------------------------------------
# export KONDUIT_COMMAND_LOGS_DIR=${KONDUIT_WORKING_DIR}/command_logs
# -------------------------------------------------------------------------------------

## Sets the directory where the file uploads are kept for Vertx BodyHandler
# -------------------------------------------------------------------------------------
# export KONDUIT_FILE_UPLOADS_DIR=/tmp
# -------------------------------------------------------------------------------------