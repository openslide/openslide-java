package edu.cmu.cs.diamond.wholeslide;

public class WholeslideDisposedException extends WholeslideException {
    public WholeslideDisposedException() {
        super("Wholeslide object has been disposed");
    }
}
