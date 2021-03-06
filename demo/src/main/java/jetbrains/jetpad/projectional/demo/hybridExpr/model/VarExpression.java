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
package jetbrains.jetpad.projectional.demo.hybridExpr.model;

import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import jetbrains.jetpad.projectional.demo.hybridExpr.model.types.Type;
import jetbrains.jetpad.projectional.demo.hybridExpr.model.types.Types;

public class VarExpression extends Expression {
  public final Property<String> name = new ValueProperty<String>();

  public VarExpression() {
  }

  @Override
  public Type getType() {
    //todo
    return Types.OBJECT;
  }
}