package de.bsommerfeld.pathetic.api.pathing.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidatorsTest {

  private static final ValidationProcessor PASS = ctx -> true;
  private static final ValidationProcessor FAIL = ctx -> false;
  private static final EvaluationContext DUMMY_CTX = null; // validators don't dereference it here

  // -------------------------------------------------------------------------
  // Strict null rejection — see CODE_REVIEW 4.1
  // -------------------------------------------------------------------------

  @Test
  void allOfRejectsNullVarargsArray() {
    assertThrows(NullPointerException.class, () -> Validators.allOf((ValidationProcessor[]) null));
  }

  @Test
  void allOfRejectsNullElementInVarargs() {
    assertThrows(NullPointerException.class, () -> Validators.allOf(PASS, null, PASS));
  }

  @Test
  void allOfRejectsNullList() {
    assertThrows(NullPointerException.class, () -> Validators.allOf((List<ValidationProcessor>) null));
  }

  @Test
  void allOfRejectsNullElementInList() {
    List<ValidationProcessor> withNull = Arrays.asList(PASS, null, PASS);
    assertThrows(NullPointerException.class, () -> Validators.allOf(withNull));
  }

  @Test
  void anyOfRejectsNullVarargsArray() {
    assertThrows(NullPointerException.class, () -> Validators.anyOf((ValidationProcessor[]) null));
  }

  @Test
  void anyOfRejectsNullElementInVarargs() {
    assertThrows(NullPointerException.class, () -> Validators.anyOf(PASS, null));
  }

  @Test
  void anyOfRejectsNullList() {
    assertThrows(NullPointerException.class, () -> Validators.anyOf((List<ValidationProcessor>) null));
  }

  @Test
  void anyOfRejectsNullElementInList() {
    List<ValidationProcessor> withNull = Arrays.asList(FAIL, null);
    assertThrows(NullPointerException.class, () -> Validators.anyOf(withNull));
  }

  @Test
  void noneOfRejectsNullVarargsArray() {
    assertThrows(NullPointerException.class, () -> Validators.noneOf((ValidationProcessor[]) null));
  }

  @Test
  void noneOfRejectsNullElementInVarargs() {
    assertThrows(NullPointerException.class, () -> Validators.noneOf(FAIL, null, FAIL));
  }

  @Test
  void noneOfRejectsNullList() {
    assertThrows(NullPointerException.class, () -> Validators.noneOf((List<ValidationProcessor>) null));
  }

  @Test
  void noneOfRejectsNullElementInList() {
    List<ValidationProcessor> withNull = Arrays.asList(FAIL, null);
    assertThrows(NullPointerException.class, () -> Validators.noneOf(withNull));
  }

  @Test
  void notRejectsNullWithNpe() {
    // Contract changed from IllegalArgumentException to NullPointerException — see CODE_REVIEW 4.1.
    assertThrows(NullPointerException.class, () -> Validators.not(null));
  }

  // -------------------------------------------------------------------------
  // Empty inputs remain allowed (identity element semantics)
  // -------------------------------------------------------------------------

  @Test
  void allOfWithNoArgumentsReturnsTrue() {
    assertTrue(Validators.allOf().isValid(DUMMY_CTX));
  }

  @Test
  void allOfWithEmptyListReturnsTrue() {
    assertTrue(Validators.allOf(Collections.emptyList()).isValid(DUMMY_CTX));
  }

  @Test
  void anyOfWithNoArgumentsReturnsFalse() {
    assertFalse(Validators.anyOf().isValid(DUMMY_CTX));
  }

  @Test
  void anyOfWithEmptyListReturnsFalse() {
    assertFalse(Validators.anyOf(Collections.emptyList()).isValid(DUMMY_CTX));
  }

  @Test
  void noneOfWithNoArgumentsReturnsTrue() {
    assertTrue(Validators.noneOf().isValid(DUMMY_CTX));
  }

  @Test
  void noneOfWithEmptyListReturnsTrue() {
    assertTrue(Validators.noneOf(Collections.emptyList()).isValid(DUMMY_CTX));
  }

  // -------------------------------------------------------------------------
  // Functional behavior of combinators (sanity)
  // -------------------------------------------------------------------------

  @Test
  void allOfShortCircuitsOnFirstFail() {
    int[] callCount = {0};
    ValidationProcessor counting =
        ctx -> {
          callCount[0]++;
          return true;
        };
    ValidationProcessor composite = Validators.allOf(FAIL, counting);
    assertFalse(composite.isValid(DUMMY_CTX));
    assertEquals(0, callCount[0], "second validator must not be called after first fails");
  }

  @Test
  void anyOfShortCircuitsOnFirstPass() {
    int[] callCount = {0};
    ValidationProcessor counting =
        ctx -> {
          callCount[0]++;
          return false;
        };
    ValidationProcessor composite = Validators.anyOf(PASS, counting);
    assertTrue(composite.isValid(DUMMY_CTX));
    assertEquals(0, callCount[0], "second validator must not be called after first passes");
  }

  @Test
  void notInvertsChild() {
    assertFalse(Validators.not(PASS).isValid(DUMMY_CTX));
    assertTrue(Validators.not(FAIL).isValid(DUMMY_CTX));
  }
}
