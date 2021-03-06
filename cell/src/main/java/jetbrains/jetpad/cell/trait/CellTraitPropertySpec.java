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
package jetbrains.jetpad.cell.trait;

import com.google.common.base.Function;
import jetbrains.jetpad.cell.Cell;

public class CellTraitPropertySpec<ValueT> {
  private String myName;
  private Function<Cell, ValueT> myDefaultValue;

  public CellTraitPropertySpec(String name) {
    this(name, (ValueT) null);
  }

  public CellTraitPropertySpec(String name, final ValueT defaultValue) {
    this(name, new Function<Cell, ValueT>() {
      @Override
      public ValueT apply(Cell cell) {
        return defaultValue;
      }
    });
  }

  public CellTraitPropertySpec(String name, Function<Cell, ValueT> defaultValue) {
    myName = name;
    myDefaultValue = defaultValue;
  }

  public ValueT getDefaultValue(Cell cell) {
    return myDefaultValue.apply(cell);
  }

  @Override
  public String toString() {
    return myName;
  }
}