package com.treble.feasimulation.presenter;

import com.treble.feasimulation.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModelValidatorTest {

    @Test
    public void validTrussModelPasses() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 2, 0));
        data.addNode(new TrussNode(3, 1, 1));
        data.addElement(new TrussMember(1, 1, 3, 0, 0.01));
        data.addElement(new TrussMember(2, 2, 3, 0, 0.01));
        data.addElement(new TrussMember(3, 1, 2, 0, 0.01));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 2, Support.Type.ROLLER));

        ModelValidator.ValidationResult result = ModelValidator.validate(data);
        assertTrue(result.isValid(), result.getErrorMessage());
    }

    @Test
    public void noElementsFails() {
        FEAData data = new FEAData();
        data.addSupport(new Support(1, 1, Support.Type.FIXED));

        ModelValidator.ValidationResult result = ModelValidator.validate(data);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("No structural elements"));
    }

    @Test
    public void zeroLengthTrussFails() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 0, 0));
        data.addElement(new TrussMember(1, 1, 2, 0, 0.01));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));

        ModelValidator.ValidationResult result = ModelValidator.validate(data);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("zero"));
    }

    @Test
    public void isolatedSupportWithFloatingTrussFails() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 5, 0));
        data.addNode(new TrussNode(3, 6, 0));
        data.addElement(new TrussMember(1, 2, 3, 0, 0.01));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));

        ModelValidator.ValidationResult result = ModelValidator.validate(data);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().toLowerCase().contains("floating"));
    }

    @Test
    public void disconnectedSupportsFail() {
        FEAData data = new FEAData();
        data.addNode(new TrussNode(1, 0, 0));
        data.addNode(new TrussNode(2, 1, 0));
        data.addNode(new TrussNode(3, 5, 0));
        data.addNode(new TrussNode(4, 6, 0));
        data.addElement(new TrussMember(1, 1, 2, 0, 0.01));
        data.addElement(new TrussMember(2, 3, 4, 0, 0.01));
        data.addSupport(new Support(1, 1, Support.Type.FIXED));
        data.addSupport(new Support(2, 3, Support.Type.FIXED));

        ModelValidator.ValidationResult result = ModelValidator.validate(data);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().toLowerCase().contains("disconnected"));
    }
}
