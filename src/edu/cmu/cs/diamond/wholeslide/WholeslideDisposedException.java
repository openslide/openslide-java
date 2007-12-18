package edu.cmu.cs.diamond.wholeslide;

public class WholeslideDisposedException extends RuntimeException {
    public WholeslideDisposedException() {
        super("Wholeslide object has been disposed");
    }
}
