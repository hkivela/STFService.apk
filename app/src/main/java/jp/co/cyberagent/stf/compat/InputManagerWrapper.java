package jp.co.cyberagent.stf.compat;

import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.co.cyberagent.stf.util.InternalApi;

public class InputManagerWrapper {
    private EventInjector eventInjector;

    public InputManagerWrapper() {
        try {
            eventInjector = new InputManagerEventInjector();
        }
        catch (UnsupportedOperationException e) {
            eventInjector = new WindowManagerEventInjector();
        }
    }

    public boolean injectKeyEvent(KeyEvent event) {
        return eventInjector.injectKeyEvent(event);
    }
    public boolean injectPointerEvent(MotionEvent event) { return eventInjector.injectPointerEvent(event); }

    private interface EventInjector {
        public boolean injectKeyEvent(KeyEvent event);
        public boolean injectPointerEvent(MotionEvent event);
    }

    /**
     * EventInjector for SDK >=16
     */
    private class InputManagerEventInjector implements EventInjector {
        private Object inputManager;
        private Method injector;
        private Method setSource;

        public InputManagerEventInjector() {
            try {
                inputManager = InternalApi.getSingleton("android.hardware.input.InputManager");

                // injectInputEvent() is @hidden
                injector = inputManager.getClass()
                        // public boolean injectInputEvent(InputEvent event, int mode)
                        .getMethod("injectInputEvent", InputEvent.class, int.class);

            }
            catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException("InputManagerEventInjector is not supported");
            }

            try {
                setSource = MotionEvent.class.getMethod("setSource", int.class);
            }
            catch (NoSuchMethodException e) {
            }

        }

        public boolean injectKeyEvent(KeyEvent event) {
            try {
                injector.invoke(inputManager, event, 0);
                return true;
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean injectPointerEvent(MotionEvent event) {
            if (setSource != null && (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0) {
                try {
                    setSource.invoke(event, InputDevice.SOURCE_TOUCHSCREEN);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return false;
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            try {
                injector.invoke(inputManager, event, 0);
                return true;
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * EventInjector for SDK <16
     */
    private class WindowManagerEventInjector implements EventInjector {
        private Object windowManager;
        private Method keyInjector;
        private Method pointerInjector;

        public WindowManagerEventInjector() {
            try {
                windowManager = WindowManagerWrapper.getWindowManager();

                keyInjector = windowManager.getClass()
                    // public boolean injectKeyEvent(android.view.KeyEvent ev, boolean sync)
                    // throws android.os.RemoteException
                    .getMethod("injectKeyEvent", KeyEvent.class, boolean.class);
                pointerInjector = windowManager.getClass()
                    .getMethod("injectPointerEvent", MotionEvent.class, boolean.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException("WindowManagerEventInjector is not supported");
            }
        }

        public boolean injectKeyEvent(KeyEvent event) {
            try {
                keyInjector.invoke(windowManager, event, false);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean injectPointerEvent(MotionEvent event) {
            try {
                pointerInjector.invoke(windowManager, event, false);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
