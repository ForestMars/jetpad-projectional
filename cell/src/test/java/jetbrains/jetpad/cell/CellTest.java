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
package jetbrains.jetpad.cell;

import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.base.Value;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.cell.event.FocusEvent;
import jetbrains.jetpad.cell.trait.BaseCellTrait;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CellTest {
  CellContainer container = new CellContainer();

  @Test
  public void attachToViewContainer() {
    TextCell v = new TextCell();

    assertNull(v.container());

    container.root.children().add(v);

    assertSame(container, v.container());
  }

  @Test
  public void detachLeadsToFocusNulling() {
    TextCell v = new TextCell();
    v.focusable().set(true);
    container.root.children().add(v);

    v.focus();
    assertSame(v, container.focusedCell.get());

    v.removeFromParent();

    assertNull(container.focusedCell.get());
  }

  @Test
  public void onDetachFocusLostSentToParent() {

    TextCell v = new TextCell();
    v.focusable().set(true);
    container.root.children().add(v);

    v.focus();

    final Value<Boolean> focusLostCalled = new Value<Boolean>(false);

    container.root.addTrait(new BaseCellTrait() {
      @Override
      public void onFocusLost(Cell cell, FocusEvent event) {
        super.onFocusLost(cell, event);
        focusLostCalled.set(true);
      }
    });
    v.removeFromParent();

    assertTrue(focusLostCalled.get());
  }

  @Test
  public void focusLeadsToFocusedSet() {
    TextCell v = new TextCell();
    v.focusable().set(true);
    container.root.children().add(v);

    v.focus();

    assertTrue(v.focused().get());
  }

  @Test(expected = IllegalStateException.class)
  public void cantFocusDetached() {
    TextCell v = new TextCell();
    v.focusable().set(true);
    v.focus();
  }

  @Test(expected = IllegalStateException.class)
  public void cantFocusNonFocusable() {
    TextCell v = new TextCell();

    container.root.children().add(v);

    v.focus();
  }

  @Test(expected = IllegalStateException.class)
  public void cantFocusInvisible() {
    TextCell v = new TextCell();
    v.focusable().set(true);
    v.visible().set(false);

    container.root.children().add(v);

    v.focus();
  }

  @Test(expected = IllegalStateException.class)
  public void cantSetFocusForUnFocusable() {
    TextCell v = new TextCell();
    container.root.children().add(v);

    container.focusedCell.set(v);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantFocusFromDifferentContainer() {
    CellContainer container2 = new CellContainer();
    TextCell v = new TextCell();
    v.focusable().set(true);
    container.root.children().add(v);

    container2.focusedCell.set(v);
  }

  @Test
  public void focusedProperty() {
    TextCell v = new TextCell();
    v.focusable().set(true);
    container.root.children().add(v);

    assertFalse(v.focused().get());
    v.focus();

    assertTrue(v.focused().get());
  }

  @Test
  public void externalProperties() {
    CellPropertySpec<String> prop = new CellPropertySpec<String>("aaa");

    TextCell v = new TextCell();

    assertNull(v.get(prop));

    v.set(prop, "z");

    assertEquals("z", v.get(prop));
  }

  @Test
  public void externalPropertiesAsObjects() {
    CellPropertySpec<String> prop = new CellPropertySpec<String>("aaa");

    TextCell v = new TextCell();
    Property<String> property = v.getProp(prop);

    assertNull(property.get());

    property.set("z");

    assertEquals("z", property.get());
  }

  @Test
  public void cellParentProperty() {
    HorizontalCell parent = new HorizontalCell();
    TextCell child = new TextCell();

    EventHandler<PropertyChangeEvent<Cell>> eh = mock(EventHandler.class);
    child.parent().addHandler(eh);

    parent.children().add(child);

    verify(eh).onEvent(new PropertyChangeEvent<Cell>(null, parent));
  }

  @Test(expected = IllegalStateException.class)
  public void addingSameChildTwiceNotAllowed() {
    HorizontalCell parent = new HorizontalCell();
    TextCell child = new TextCell();

    parent.children().addAll(Arrays.asList(child, child));
  }

}