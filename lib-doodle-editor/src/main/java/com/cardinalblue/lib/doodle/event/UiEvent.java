// Copyright (c) 2017-present boyw165
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.cardinalblue.lib.doodle.event;

public class UiEvent<T> {

    public final boolean justStart, doing;
    public final boolean isTriggeredByUser;
    public final T bundle;

    public static <T> UiEvent<T> start(T bundle) {
        return new UiEvent<>(true, false, true, bundle);
    }

    public static <T> UiEvent<T> doing(boolean isTriggeredByUser,
                                       T bundle) {
        return new UiEvent<>(false, true,
                             isTriggeredByUser,
                             bundle);
    }

    public static <T> UiEvent<T> stop(T bundle) {
        return new UiEvent<>(false, false, true, bundle);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    protected UiEvent(boolean justStart,
                      boolean doing,
                      boolean isTriggeredByUser,
                      T bundle) {
        this.justStart = justStart;
        this.doing = doing;
        this.isTriggeredByUser = isTriggeredByUser;
        this.bundle = bundle;
    }
}