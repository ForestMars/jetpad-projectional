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
package jetbrains.jetpad.projectional.view.gwt;

import com.google.common.base.Strings;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.user.client.DOM;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import jetbrains.jetpad.base.Handler;
import jetbrains.jetpad.base.Value;
import jetbrains.jetpad.event.*;
import jetbrains.jetpad.event.dom.ClipboardSupport;
import jetbrains.jetpad.event.dom.EventTranslator;
import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.geometry.Vector;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.model.event.CompositeRegistration;
import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.event.Registration;
import jetbrains.jetpad.model.property.*;
import jetbrains.jetpad.projectional.domUtil.Scrolling;
import jetbrains.jetpad.projectional.domUtil.TextMetrics;
import jetbrains.jetpad.projectional.domUtil.TextMetricsCalculator;
import jetbrains.jetpad.projectional.view.View;
import jetbrains.jetpad.projectional.view.ViewContainer;
import jetbrains.jetpad.projectional.view.spi.ViewContainerPeer;

import static com.google.gwt.query.client.GQuery.$;

public class View2Dom {
  public static final String FONT_FAMILY = "monospace";
  public static final int FONT_SIZE = 15;

  public static String font() {
    return FONT_SIZE + "px " + FONT_FAMILY;
  }

  public static Registration showDemo(final ViewContainer container, final Element element) {
    CompositeRegistration reg = new CompositeRegistration();

    final Element rootDiv = DOM.createDiv();
    disablePopup(rootDiv);
    element.appendChild(rootDiv);
    rootDiv.setTabIndex(0);

    final Style rootDivStyle = rootDiv.getStyle();
    rootDivStyle.setPosition(Style.Position.RELATIVE);
    rootDivStyle.setPadding(0, Style.Unit.PX);

    final ValueProperty<Rectangle> visibleArea = new ValueProperty<Rectangle>();
    final Runnable update = new Runnable() {
      @Override
      public void run() {
        Rectangle newRect = new Rectangle(Window.getScrollLeft() - rootDiv.getAbsoluteLeft(), Window.getScrollTop() - rootDiv.getAbsoluteTop(), Window.getClientWidth(), Window.getClientHeight());
        if (visibleArea.get() != null && visibleArea.get().contains(newRect)) return;
        visibleArea.set(expand(newRect));
      }

      private Rectangle expand(Rectangle rect) {
        return new Rectangle(
          rect.origin.x, rect.origin.y - rect.dimension.y / 2,
          rect.dimension.x, 2 * rect.dimension.y
        );
      }
    };
    update.run();


    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        update.run();
      }
    });

    Window.addWindowScrollHandler(new Window.ScrollHandler() {
      @Override
      public void onWindowScroll(Window.ScrollEvent event) {
        update.run();
      }
    });


    View2DomContext ctx = new View2DomContext() {
      @Override
      public ReadableProperty<Rectangle> visibleArea() {
        return visibleArea;
      }
    };

    final Mapper<? extends View,? extends Element> rootMapper = ViewMapperFactory.factory(ctx).createMapper(container.root());
    rootMapper.attachRoot();
    rootDiv.appendChild(rootMapper.getTarget());

    TextMetrics metrics = TextMetricsCalculator.calculate(FONT_FAMILY, FONT_SIZE, "x");
    final int baseLine = metrics.baseLine();
    final int fontWidth = metrics.dimension().x;
    final int fontHeight = metrics.dimension().y;

    final ClipboardSupport clipboardSupport = new ClipboardSupport(rootDiv);

    container.setPeer(new ViewContainerPeer() {
      private Registration myReg;

      @Override
      public void attach(final ViewContainer container) {
        myReg = container.root().valid().addHandler(new EventHandler<PropertyChangeEvent<Boolean>>() {
          @Override
          public void onEvent(PropertyChangeEvent<Boolean> event) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
              @Override
              public void execute() {
                container.root().validate();
              }
            });
          }
        });

        container.root().validate();
      }

      @Override
      public void detach() {
        myReg.remove();
      }

      @Override
      public void repaint(View view) {
      }

      @Override
      public Rectangle visibleRect() {
        container.root().validate();
        return container.root().bounds().get();
      }

      @Override
      public void scrollTo(View view) {
        Scrolling.scrollTo((Element) rootMapper.getDescendantMapper(view).getTarget());
      }

      @Override
      public void boundsChanged(View view, PropertyChangeEvent<Rectangle> change) {
      }

      @Override
      public int textHeight() {
        return fontHeight;
      }

      @Override
      public int textBaseLine() {
        return baseLine;
      }

      @Override
      public int textWidth(String text) {
        return text.length() * fontWidth;
      }
    });

    reg.add(PropertyBinding.bind(container.root().bounds(), new WritableProperty<Rectangle>() {
      @Override
      public void set(Rectangle value) {
        Vector bottomRight = value.origin.add(value.dimension);
        rootDivStyle.setWidth(bottomRight.x, Style.Unit.PX);
        rootDivStyle.setHeight(bottomRight.y, Style.Unit.PX);
      }
    }));

    final Value<Boolean> pressed = new Value<Boolean>(false);
    final Value<Boolean> pressedOutside = new Value<Boolean>(false);

    reg.add(eventRegistration(Event.ONMOUSEDOWN, $(rootDiv).mousedown(new Function() {
      @Override
      public boolean f(Event e) {
        pressed.set(true);
        rootDiv.focus();
        container.mousePressed(toMouseEvent(rootDiv, e));
        pressedOutside.set(false);
        return false;
      }
    })));
    reg.add(eventRegistration(Event.ONMOUSEDOWN, $(Document.get()).mousedown(new Function() {
      @Override
      public boolean f(Event e) {
        pressed.set(true);
        MouseEvent evt = toMouseEvent(rootDiv, e);
        if (!isContainerEvent(evt, container)) {
          pressedOutside.set(true);
        }
        return true;
      }
    })));

    reg.add(eventRegistration(Event.ONMOUSEUP, $(Document.get()).mouseup(new Function() {
      @Override
      public boolean f(Event e) {
        pressed.set(false);
        return true;
      }
    })));
    reg.add(eventRegistration(Event.ONMOUSEUP, $(rootDiv).mouseup(new Function() {
      @Override
      public boolean f(Event e) {
        pressed.set(false);
        container.mouseReleased(toMouseEvent(rootDiv, e));
        return false;
      }
    })));

    reg.add(eventRegistration(Event.ONMOUSEMOVE, $(Document.get()).mousemove(new Function() {
      @Override
      public boolean f(Event e) {
        MouseEvent evt = toMouseEvent(rootDiv, e);
        if (pressed.get() && !pressedOutside.get()) {
          container.mouseDragged(evt);
        }
        return true;
      }
    })));
    reg.add(eventRegistration(Event.ONMOUSEMOVE, $(rootDiv).mousemove(new Function() {
      @Override
      public boolean f(Event e) {
        MouseEvent evt = toMouseEvent(rootDiv, e);
        if (pressed.get() && !pressedOutside.get()) {
          container.mouseDragged(evt);
        } else {
          container.mouseMoved(evt);
        }
        return true;
      }
    })));

    reg.add(eventRegistration(Event.ONKEYDOWN, $(rootDiv).keydown(new Function() {
      @Override
      public boolean f(Event e) {
        return EventTranslator.dispatchKeyPress(e, new Handler<KeyEvent>() {
          @Override
          public void handle(final KeyEvent e) {
            if (e.is(Key.V, ModifierKey.CONTROL) || e.is(Key.V, ModifierKey.META)) {
              clipboardSupport.pasteContent(new Handler<String>() {
                @Override
                public void handle(String text) {
                  if (Strings.isNullOrEmpty(text)) {
                    container.keyPressed(new KeyEvent(e.key(), e.keyChar(), e.modifiers()));
                  } else {
                    container.paste(new PasteEvent(new TextClipboardContent(text)));
                  }
                }
              });
              return;
            }

            if (e.is(Key.C, ModifierKey.CONTROL) || e.is(Key.C, ModifierKey.META) || e.is(Key.X, ModifierKey.CONTROL) || e.is(Key.X, ModifierKey.META)) {
              CopyCutEvent copyEvent;
              if (e.key() == Key.X) {
                container.cut(copyEvent = new CopyCutEvent(true));
              } else {
                container.copy(copyEvent = new CopyCutEvent(false));
              }
              ClipboardContent content = copyEvent.getResult();
              if (content != null) {
                clipboardSupport.copyContent(content);
              }
              return;
            }

            container.keyPressed(e);
          }
        });
      }
    })));
    reg.add(eventRegistration(Event.ONKEYUP, $(rootDiv).keyup(new Function() {
      @Override
      public boolean f(Event e) {
        return EventTranslator.dispatchKeyRelease(e, new Handler<KeyEvent>() {
          @Override
          public void handle(KeyEvent e) {
            container.keyReleased(e);
          }
        });
      }
    })));
    reg.add(eventRegistration(Event.ONKEYPRESS, $(rootDiv).keypress(new Function() {
      @Override
      public boolean f(Event e) {
        return EventTranslator.dispatchKeyType(e, new Handler<KeyEvent>() {
          @Override
          public void handle(KeyEvent e) {
            container.keyTyped(e);
          }
        });
      }
    })));
    reg.add(new Registration() {
      @Override
      public void remove() {
        rootMapper.detachRoot();
      }
    });
    return reg;
  }

  private static Registration eventRegistration(final int event, final GQuery query) {
    return new Registration() {
      @Override
      public void remove() {
        query.unbind(event);
      }
    };
  }

  private static boolean isContainerEvent(MouseEvent evt, ViewContainer container) {
    return container.contentRoot().bounds().get().contains(evt.location());
  }

  private static MouseEvent toMouseEvent(Element el, Event e) {
    int cx = e.getClientX();
    int cy = e.getClientY();

    int scrollLeft = Window.getScrollLeft();
    int scrollTop = Window.getScrollTop();

    int absoluteLeft = el.getAbsoluteLeft();
    int absoluteTop = el.getAbsoluteTop();

    int elScrollTop = el.getScrollTop();
    int elScrollLeft = el.getScrollLeft();

    int x = cx + scrollLeft - absoluteLeft + elScrollLeft;
    int y = cy + scrollTop - absoluteTop + elScrollTop;

    return new MouseEvent(x, y);
  }

  private static native void disablePopup(Element el) /*-{
    el.oncontextmenu = function() {
      return false;
    }
  }-*/;
}