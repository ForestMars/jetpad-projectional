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
package jetbrains.jetpad.cell.indent;

import jetbrains.jetpad.model.collections.CollectionItemEvent;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.CellPropertySpec;

public interface IndentContainerCellListener {
  void childAdded(CollectionItemEvent<Cell> event);
  void childRemoved(CollectionItemEvent<Cell> event);
  void propertyChanged(Cell cell, CellPropertySpec<?> prop, PropertyChangeEvent<?> event);
}