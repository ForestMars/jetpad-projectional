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

import com.google.common.base.Objects;
import jetbrains.jetpad.event.*;
import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.geometry.Vector;
import jetbrains.jetpad.model.collections.CollectionItemEvent;
import jetbrains.jetpad.model.collections.CollectionListener;
import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.composite.*;
import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.event.ListenerCaller;
import jetbrains.jetpad.model.event.Listeners;
import jetbrains.jetpad.model.event.Registration;
import jetbrains.jetpad.model.property.BaseReadableProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.model.property.ReadableProperty;
import jetbrains.jetpad.cell.event.CellEventSpec;
import jetbrains.jetpad.cell.event.CompletionEvent;
import jetbrains.jetpad.cell.event.EventPriority;
import jetbrains.jetpad.cell.event.FocusEvent;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.trait.CellTraitEventSpec;
import jetbrains.jetpad.cell.trait.CellTraitPropertySpec;
import jetbrains.jetpad.values.Color;
import jetbrains.jetpad.model.util.ListMap;

import java.util.*;

public abstract class Cell implements Composite<Cell>, HasVisibility, HasFocusability, HasBounds {
  public static final Color HIGHLIGHT_COLOR = new Color(200, 200, 200);
  public static final Color SELECTION_COLOR = Color.LIGHT_BLUE;

  public static final CellPropertySpec<Boolean> FOCUSED = new CellPropertySpec<Boolean>("focused", false);

  public static final CellPropertySpec<Cell> BOTTOM_POPUP = new CellPropertySpec<Cell>("bottomPopup");
  public static final CellPropertySpec<Cell> FRONT_POPUP = new CellPropertySpec<Cell>("frontPopup");
  public static final CellPropertySpec<Cell> LEFT_POPUP = new CellPropertySpec<Cell>("leftPopup");
  public static final CellPropertySpec<Cell> RIGHT_POPUP = new CellPropertySpec<Cell>("rightPopup");

  private static final CellPropertySpec<Cell>[] POPUP_SPECS = new CellPropertySpec[]{LEFT_POPUP, RIGHT_POPUP, BOTTOM_POPUP, FRONT_POPUP};

  public static final CellPropertySpec<Boolean> VISIBLE = new CellPropertySpec<Boolean>("visible", true);
  public static final CellPropertySpec<Boolean> SELECTED = new CellPropertySpec<Boolean>("selected", false);
  public static final CellPropertySpec<Boolean> HIGHLIGHTED = new CellPropertySpec<Boolean>("highlighted", false);
  public static final CellPropertySpec<Boolean> FOCUSABLE = new CellPropertySpec<Boolean>("focusable", false);
  public static final CellPropertySpec<Color> BACKGROUND = new CellPropertySpec<Color>("background");
  public static final CellPropertySpec<Color> BORDER_COLOR = new CellPropertySpec<Color>("borderColor");

  public static boolean isPopupProp(CellPropertySpec<?> prop) {
    return prop == BOTTOM_POPUP || prop == FRONT_POPUP || prop == LEFT_POPUP || prop == RIGHT_POPUP;
  }

  private CellTrait[] myCellTraits = CellTrait.EMPTY_ARRAY;
  private List<Cell> myChildren;
  private CellContainer myContainer;
  private Cell myParent;
  private ListMap<CellPropertySpec<?>, Object> myProperties;
  private Listeners<CellListener> myListeners;

  protected Cell() {
    this(null);
  }

  Cell(CellContainer cellContainer) {
    if (cellContainer != null) {
      attach(cellContainer);
    }
  }

  public ObservableList<Cell> children() {
    return new ExternalChildList();
  }

  public Property<Boolean> visible() {
    return getProp(VISIBLE);
  }

  public Property<Boolean> selected() {
    return getProp(SELECTED);
  }

  public Property<Boolean> highlighted() {
    return getProp(HIGHLIGHTED);
  }

  public Property<Boolean> focusable() {
    return getProp(FOCUSABLE);
  }

  public Property<Color> background() {
    return getProp(BACKGROUND);
  }

  public Property<Color> borderColor() {
    return getProp(BORDER_COLOR);
  }

  public Property<Cell> bottomPopup() {
    return getProp(BOTTOM_POPUP);
  }

  public Property<Cell> frontPopup() {
    return getProp(FRONT_POPUP);
  }

  public Property<Cell> leftPopup() {
    return getProp(LEFT_POPUP);
  }

  public Property<Cell> rightPopup() {
    return getProp(RIGHT_POPUP);
  }

  public <EventT extends Event> void dispatch(EventT e, CellEventSpec<EventT> spec) {
    dispatchStep(e, spec);
    if (e.isConsumed()) return;

    if (parent().get() != null) {
      parent().get().dispatch(e, spec);
    }
  }

  private <EventT extends Event> void dispatchStep(EventT e, CellEventSpec<EventT> spec) {
    if (spec == CellEventSpec.KEY_PRESSED || spec == CellEventSpec.KEY_RELEASED || spec == CellEventSpec.KEY_TYPED) {
      for (EventPriority p : EventPriority.values()) {
        for (CellTrait t : myCellTraits) {
          if (p == EventPriority.LOW) {
            if (spec == CellEventSpec.KEY_PRESSED) {
              t.onKeyPressedLowPriority(this, (KeyEvent) e);
            } else if (spec == CellEventSpec.KEY_RELEASED) {
              t.onKeyReleasedLowPriority(this, (KeyEvent) e);
            } else {
              t.onKeyTypedLowPriority(this, (KeyEvent) e);
            }
          } else {
            if (spec == CellEventSpec.KEY_PRESSED) {
              t.onKeyPressed(this, (KeyEvent) e);
            } else if (spec == CellEventSpec.KEY_RELEASED) {
              t.onKeyReleased(this, (KeyEvent) e);
            } else {
              t.onKeyTyped(this, (KeyEvent) e);
            }
          }
          if (e.isConsumed()) return;
        }
      }
    } else {
      for (CellTrait t : myCellTraits) {
        if (spec == CellEventSpec.FOCUS_GAINED) {
          t.onFocusGained(this, (FocusEvent) e);
        } else if (spec == CellEventSpec.FOCUS_LOST) {
          t.onFocusLost(this, (FocusEvent) e);
        } else if (spec == CellEventSpec.MOUSE_PRESSED) {
          t.onMousePressed(this, (MouseEvent) e);
        } else if (spec == CellEventSpec.MOUSE_RELEASED) {
          t.onMouseReleased(this, (MouseEvent) e);
        } else if (spec == CellEventSpec.MOUSE_MOVED) {
          t.onMouseMoved(this, (MouseEvent) e);
        } else if (spec == CellEventSpec.COPY) {
          t.onCopy(this, (CopyCutEvent) e);
        } else if (spec == CellEventSpec.CUT) {
          t.onCut(this, (CopyCutEvent) e);
        } else if (spec == CellEventSpec.PASTE) {
          t.onPaste(this, (PasteEvent) e);
        } else if (spec == CellEventSpec.COMPLETE) {
          t.onComplete(this, (CompletionEvent) e);
        } else {
          throw new IllegalStateException("Unknown even type " + e);
        }

        if (e.isConsumed()) return;
      }
    }
  }

  public <EventT extends Event> void dispatch(EventT e, CellTraitEventSpec<EventT> spec) {
    for (CellTrait t : myCellTraits) {
      t.onViewTraitEvent(this, spec, e);
      if (e.isConsumed()) return;
    }

    if (parent().get() != null && spec.isBubbling()) {
      parent().get().dispatch(e, spec);
    }
  }

  public Registration addTrait(final CellTrait trait) {
    //todo we might change properties here. need to fire events
    CellTrait[] newTraits = new CellTrait[myCellTraits.length + 1];
    newTraits[0] = trait;
    System.arraycopy(myCellTraits, 0, newTraits, 1, myCellTraits.length);
    myCellTraits = newTraits;
    return new Registration() {
      @Override
      public void remove() {
        int index = Arrays.asList(myCellTraits).indexOf(trait);
        CellTrait[] newTraits = new CellTrait[myCellTraits.length - 1];
        System.arraycopy(myCellTraits, 0, newTraits, 0, index);
        System.arraycopy(myCellTraits, index + 1, newTraits, index, myCellTraits.length - index - 1);
        myCellTraits = newTraits;
      }
    };
  }

  public ReadableProperty<Cell> parent() {
    return new BaseReadableProperty<Cell>() {
      @Override
      public Cell get() {
        return myParent;
      }

      @Override
      public Registration addHandler(final EventHandler<? super PropertyChangeEvent<Cell>> handler) {
        return addListener(new CellAdapter() {
          @Override
          public void onParentChanged(PropertyChangeEvent<Cell> event) {
            handler.onEvent(event);
          }
        });
      }
    };
  }

  public CellContainer container() {
    return myContainer;
  }

  public ReadableProperty<Boolean> focused() {
    return getProp(FOCUSED);
  }

  boolean canFocus() {
    if (!focusable().get()) return false;
    if (myContainer == null) return false;
    if (!visible().get()) return false;
    return true;
  }

  public void focus() {
    if (!canFocus()) {
      throw new IllegalStateException();
    }
    myContainer.focusedCell.set(this);
  }

  public Vector origin() {
    return getViewContainerPeer().getBounds(this).origin;
  }

  public Vector dimension() {
    return getViewContainerPeer().getBounds(this).dimension;
  }

  public Rectangle getBounds() {
    return getViewContainerPeer().getBounds(this);
  }

  public void scrollTo() {
    getViewContainerPeer().scrollTo(this);
  }

  protected CellContainerPeer getViewContainerPeer() {
    if (myContainer == null) throw new IllegalStateException();
    return myContainer.getCellContainerPeer();
  }

  public Registration addListener(CellListener l) {
    if (myListeners == null) {
      myListeners = new Listeners<CellListener>();
    }
    return myListeners.add(l);
  }

  public <ValueT> ValueT get(CellPropertySpec<ValueT> prop) {
    if (myProperties == null || !myProperties.containsKey(prop)) {
      return getDefaultValue(prop);
    }
    return (ValueT) myProperties.get(prop);
  }

  public <ValueT> Registration set(final CellPropertySpec<ValueT> prop, ValueT value) {
    final ValueT old = get(prop);

    if (Objects.equal(value, old)) return Registration.EMPTY;

    final PropertyChangeEvent<ValueT> event = new PropertyChangeEvent<ValueT>(old, value);

    beforePropertySet(prop, event);

    if (Objects.equal(value, getDefaultValue(prop))) {
      myProperties.remove(prop);
      if (myProperties.isEmpty()) {
        myProperties = null;
      }
    } else {
      if (myProperties == null) {
        myProperties = new ListMap<CellPropertySpec<?>, Object>();
      }
      myProperties.put(prop, value);
    }

    propertySet(prop, event);
    onPropertySet(prop, event);

    for (CellTrait t : myCellTraits) {
      t.onPropertyChanged(this, prop, event);
    }

    if (myListeners != null) {
      myListeners.fire(new ListenerCaller<CellListener>() {
        @Override
        public void call(CellListener l) {
          l.onPropertyChanged(prop, event);
        }
      });
    }

    if (myContainer != null) {
      myContainer.viewPropertyChanged(this, prop, event);
    }

    return new Registration() {
      @Override
      public void remove() {
        set(prop, old);
      }
    };
  }

  private <ValueT> ValueT getDefaultValue(CellPropertySpec<ValueT> prop) {
    for (CellTrait t : myCellTraits) {
      Object result = t.get(this, prop);
      if (result == CellTrait.NULL) return null;
      if (result != null) {
        return (ValueT) result;
      }
    }

    return prop.getDefault(this);
  }

  public <ValueT> ValueT get(CellTraitPropertySpec<ValueT> prop) {
    ValueT result = getRaw(prop);
    if (result != null) return result;
    return prop.getDefaultValue(this);
  }

  public <ValueT> ValueT getRaw(CellTraitPropertySpec<ValueT> prop) {
    for (CellTrait t : myCellTraits) {
      Object result = t.get(this, prop);
      if (result == CellTrait.NULL) return null;
      if (result != null) {
        return (ValueT) result;
      }
    }
    return null;
  }

  public <ValueT> Property<ValueT> getProp(final CellPropertySpec<ValueT> prop) {
    class MyProperty extends BaseReadableProperty<ValueT> implements Property<ValueT> {
      @Override
      public ValueT get() {
        return Cell.this.get(prop);
      }

      @Override
      public void set(ValueT value) {
        Cell.this.set(prop, value);
      }

      @Override
      public Registration addHandler(final EventHandler<? super PropertyChangeEvent<ValueT>> handler) {
        return addListener(new CellAdapter() {
          @Override
          public void onPropertyChanged(CellPropertySpec<?> viewProp, PropertyChangeEvent<?> event) {
            if (!Objects.equal(prop, viewProp)) return;
            handler.onEvent((PropertyChangeEvent<ValueT>) event);
          }
        });
      }

      @Override
      public String getPropExpr() {
        return Cell.this + "." + prop;
      }
    }

    return new MyProperty();
  }

  public List<Cell> popups() {
    if (myProperties == null) return Collections.emptyList();
    List<Cell> result = new ArrayList<Cell>();
    for (CellPropertySpec<Cell> ps : POPUP_SPECS) {
      Cell cell = (Cell) myProperties.get(ps);
      if (cell != null) {
        result.add(cell);
      }
    }
    return result;
  }

  public boolean isAttached() {
    return myContainer != null;
  }

  private void attach(CellContainer container) {
    if (container == null) throw new IllegalStateException();
    if (myContainer != null) throw new IllegalStateException();

    myContainer = container;
    myContainer.viewAdded(this);

    if (myChildren != null) {
      for (Cell child : myChildren) {
        child.attach(container);
      }
    }
    for (Cell popup : popups()) {
      popup.attach(container);
    }
  }

  private void detach() {
    if (myContainer == null) throw new IllegalStateException();

    myContainer.viewRemoved(this);

    myContainer = null;

    if (myChildren != null) {
      for (Cell child : myChildren) {
        child.detach();
      }
    }
    for (Cell popup : popups()) {
      popup.detach();
    }
  }

  public void removeFromParent() {
    if (myParent == null) throw new IllegalStateException();

    if (myParent.myChildren != null && myParent.myChildren.contains(this)) {
      myParent.children().remove(this);
      return;
    }

    for (Property<Cell> popup : Arrays.asList(myParent.leftPopup(), myParent.rightPopup(), myParent.bottomPopup(), myParent.frontPopup())) {
      if (popup.get() == this) {
        popup.set(null);
        return;
      }
    }

    throw new IllegalStateException();
  }

  private <ValueT> void beforePropertySet(CellPropertySpec<ValueT> prop, PropertyChangeEvent<ValueT> event) {
    if (isPopupProp(prop)) {
      PropertyChangeEvent<Cell> cellChangeEvent = (PropertyChangeEvent<Cell>) event;
      Cell oldValue = cellChangeEvent.getOldValue();
      Cell value = cellChangeEvent.getNewValue();

      CellContainer cellContainer = myContainer;
      if (cellContainer != null) {
        if (oldValue != null) {
          cellContainer.popupRemoved(oldValue);
          oldValue.changeParent(null);
        }
        if (value != null) {
          cellContainer.popupAdded(value);
          value.changeParent(Cell.this);
        }
      }
    }
  }

  private <ValueT> void propertySet(CellPropertySpec<ValueT> prop, PropertyChangeEvent<ValueT> event) {
    if (isPopupProp(prop)) {
      PropertyChangeEvent<Cell> cellChangeEvent = (PropertyChangeEvent<Cell>) event;
      Cell oldValue = cellChangeEvent.getOldValue();
      Cell value = cellChangeEvent.getNewValue();
      if (isAttached()) {
        if (oldValue != null) {
          oldValue.detach();
        }
        if (value != null) {
          value.attach(myContainer);
        }
      }
    }
  }

  private void changeParent(Cell newParent) {
    final PropertyChangeEvent<Cell> event = myListeners != null ? new PropertyChangeEvent<Cell>(myParent, newParent) : null;
    myParent = newParent;
    if (myListeners != null) {
      myListeners.fire(new ListenerCaller<CellListener>() {
        @Override
        public void call(CellListener l) {
          l.onParentChanged(event);
        }
      });
    }
  }

  protected void onPropertySet(CellPropertySpec<?> prop, PropertyChangeEvent<?> event) {
  }

  protected void onBeforeChildAdded(CollectionItemEvent<Cell> event) {
  }

  protected void onChildAdded(CollectionItemEvent<Cell> event) {
  }

  protected void onBeforeChildRemoved(CollectionItemEvent<Cell> event) {
  }

  protected void onChildRemoved(CollectionItemEvent<Cell> event) {
  }

  @Override
  public String toString() {
    String name = getClass().getName();
    int dotIndex = name.lastIndexOf('.');
    String className = dotIndex == 1 ? name : name.substring(dotIndex + 1);
    return className + "@" + Integer.toHexString(hashCode());
  }

  private class ChildList extends ObservableArrayList<Cell> {
    @Override
    public void add(final int index, final Cell item) {
      if (item.parent().get() != null) throw new IllegalStateException();

      final CollectionItemEvent<Cell> event = new CollectionItemEvent<Cell>(item, index, true);

      onBeforeChildAdded(event);

      item.changeParent(Cell.this);

      super.add(index, item);

      if (isAttached()) {
        item.attach(myContainer);
      }

      onChildAdded(event);

      if (myContainer != null) {
        myContainer.viewChildAdded(Cell.this, event);
      }

      if (myListeners != null) {
        myListeners.fire(new ListenerCaller<CellListener>() {
          @Override
          public void call(CellListener l) {
            l.onChildAdded(event);
          }
        });
      }
    }

    @Override
    public Cell remove(final int index) {
      final Cell item = get(index);
      final CollectionItemEvent<Cell> event = new CollectionItemEvent<Cell>(item, index, false);

      if (isAttached() && myContainer.focusedCell.get() != null && Composites.isDescendant(item, myContainer.focusedCell.get())) {
        myContainer.focusedCell.set(null);
      }

      onBeforeChildRemoved(event);

      item.changeParent(null);

      Cell result = super.remove(index);

      if (isAttached()) {
        item.detach();
      }

      onChildRemoved(event);

      if (myContainer != null) {
        myContainer.viewChildRemoved(Cell.this, event);
      }

      if (myListeners != null) {
        myListeners.fire(new ListenerCaller<CellListener>() {
          @Override
          public void call(CellListener l) {
            l.onChildRemoved(event);
          }
        });
      }

      return result;
    }
  }

  private class ExternalChildList extends AbstractList<Cell> implements ObservableList<Cell> {
    @Override
    public Cell get(int index) {
      if (myChildren == null) throw new IndexOutOfBoundsException();
      return myChildren.get(index);
    }

    @Override
    public int size() {
      if (myChildren == null) return 0;
      return myChildren.size();
    }

    @Override
    public Cell set(int index, Cell element) {
      if (myChildren == null) throw new IndexOutOfBoundsException();
      return myChildren.set(index, element);
    }

    @Override
    public void add(int index, Cell element) {
      ensureChildrenInitialized();
      myChildren.add(index, element);
    }

    private void ensureChildrenInitialized() {
      if (myChildren == null) {
        myChildren = new ChildList();
      }
    }

    @Override
    public Cell remove(int index) {
      if (myChildren == null) throw new IndexOutOfBoundsException();
      Cell result = myChildren.remove(index);
      if (myChildren.isEmpty()) {
        myChildren = null;
      }
      return result;
    }

    @Override
    public Registration addListener(final CollectionListener<Cell> l) {
      return Cell.this.addListener(new CellAdapter() {
        @Override
        public void onChildAdded(CollectionItemEvent<Cell> event) {
          l.onItemAdded(event);
        }

        @Override
        public void onChildRemoved(CollectionItemEvent<Cell> event) {
          l.onItemRemoved(event);
        }
      });
    }

    @Override
    public Registration addHandler(final EventHandler<? super CollectionItemEvent<Cell>> handler) {
      return addListener(new CollectionListener<Cell>() {
        @Override
        public void onItemAdded(CollectionItemEvent<Cell> event) {
          handler.onEvent(event);
        }

        @Override
        public void onItemRemoved(CollectionItemEvent<Cell> event) {
          handler.onEvent(event);
        }
      });
    }
  }
}