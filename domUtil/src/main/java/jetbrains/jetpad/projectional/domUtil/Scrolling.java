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
package jetbrains.jetpad.projectional.domUtil;

import com.google.gwt.dom.client.Element;
import jetbrains.jetpad.geometry.Rectangle;

public class Scrolling {
  public static void scrollTo(Element element) {
    adjustScrollers(element);
    Rectangle visibleArea = new Rectangle(getScrollX(), getScrollY(), getScrollWidth(), getScrollHeight());
    Rectangle bounds = getBounds(element);
    if (!visibleArea.contains(bounds)) {
      scrollToView(element);
    }
  }

  private static Rectangle getBounds(Element element) {
    int x = element.getAbsoluteLeft();
    int y = element.getAbsoluteTop();
    int width = element.getAbsoluteRight() - element.getAbsoluteLeft();
    int height = element.getAbsoluteBottom() - element.getAbsoluteTop();
    return new Rectangle(x, y, width, height);
  }

  private static void adjustScrollers(Element element) {
    int top = element.getOffsetTop();
    int left = element.getOffsetLeft();
    int height = element.getOffsetHeight();
    int width = element.getOffsetWidth();

    while (element.getParentElement() != null) {
      Element parent = element.getParentElement();
      Element offsetParent = element.getOffsetParent();

      String scroll = parent.getStyle().getOverflow();
      if ("scroll".equals(scroll) || "auto".equals(scroll)) {
        int parentTop = parent.getScrollTop();
        int parentLeft = parent.getScrollLeft();
        int clientWidth = parent.getClientWidth();
        int clientHeight = parent.getClientHeight();

        if (top < parentTop) {
          parent.setScrollTop(top);
        }
        if (left < parentLeft) {
          parent.setScrollLeft(left);
        }
        if (top + height > parentTop + clientHeight) {
          parent.setScrollTop(top + height - clientHeight);
        }
        if (left + width > parentLeft + clientWidth) {
          parent.setScrollLeft(left + width - clientWidth);
        }
      }

      if (parent == offsetParent) {
        top += parent.getOffsetTop() - parent.getScrollTop();
        left += parent.getOffsetLeft() - parent.getScrollLeft();
      }
      element = parent;
    }
  }

  private static native int getScrollX() /*-{
    return $wnd.pageXOffset;
  }-*/;

  private static native int getScrollY() /*-{
    return $wnd.pageYOffset;
  }-*/;

  private static native int getScrollWidth() /*-{
    return $wnd.innerWidth;
  }-*/;

  private static native int getScrollHeight() /*-{
    return $wnd.innerHeight;
  }-*/;

  private static native void scrollToView(Element el) /*-{
    el.scrollIntoView(false);
  }-*/;

}