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

import jetbrains.jetpad.event.Event;

public class CellTraitEventSpec<EventT extends Event> {
  private String myName;
  private boolean myBubbling;

  public CellTraitEventSpec(String name) {
    this(name, true);
  }

  public CellTraitEventSpec(String name, boolean bubbling) {
    myName = name;
    myBubbling = bubbling;
  }

  public boolean isBubbling() {
    return myBubbling;
  }

  @Override
  public String toString() {
    return myName;
  }
}