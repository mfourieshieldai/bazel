// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.skyframe.actiongraph.v2;

import com.google.devtools.build.lib.analysis.AnalysisProtosV2.ActionGraphContainer;
import com.google.devtools.build.lib.analysis.AnalysisProtosV2.Configuration;
import com.google.devtools.build.lib.buildeventstream.BuildEvent;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;

/** Cache for BuildConfigurations in the action graph. */
public class KnownConfigurations extends BaseCache<BuildEvent, Configuration> {

  KnownConfigurations(ActionGraphContainer.Builder actionGraphBuilder) {
    super(actionGraphBuilder);
  }

  @Override
  Configuration createProto(BuildEvent config, int id) {
    BuildEventStreamProtos.Configuration configProto =
        config.asStreamProto(/*context=*/ null).getConfiguration();
    return Configuration.newBuilder()
        .setChecksum(config.getEventId().asStreamProto().getConfiguration().getId())
        .setMnemonic(configProto.getMnemonic())
        .setPlatformName(configProto.getPlatformName())
        .setId(id)
        .build();
  }

  @Override
  void addToActionGraphBuilder(Configuration configurationProto) {
    actionGraphBuilder.addConfiguration(configurationProto);
  }
}