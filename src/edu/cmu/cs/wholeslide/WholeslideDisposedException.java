package edu.cmu.cs.wholeslide;

public class WholeslideDisposedException extends WholeslideException {
    public WholeslideDisposedException() {
        super("Wholeslide object has been disposed");
    }
}
