/*
 * Copyright 2012-2013 JetBrains s.r.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.jetpad.projectional.demo.expr.mapper;

import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.demo.expr.model.ExpressionContainer;

class ExpressionContainerMapper extends Mapper<ExpressionContainer, ExpressionContainerCell> {
  ExpressionContainerMapper(ExpressionContainer source) {
    super(source, new ExpressionContainerCell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(ExprSynchronizers.expressionSynchronizer(this, getSource().expression, getTarget().expression));
  }
}