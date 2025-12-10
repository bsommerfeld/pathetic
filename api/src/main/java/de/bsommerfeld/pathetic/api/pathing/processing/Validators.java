package de.bsommerfeld.pathetic.api.pathing.processing;

import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext; // Assuming this exists
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for creating and combining {@link ValidationProcessor} instances. This class
 * provides factory methods for common boolean logic operations (AND, OR, NOT) to build complex
 * validation rules from simpler, focused validators.
 *
 * <p>All composite validators created by this class will correctly propagate the {@link
 * Processor#initializeSearch(SearchContext)} and {@link Processor#finalizeSearch(SearchContext)}
 * lifecycle calls to their underlying child validators.
 *
 * <p>Null validator instances passed within arrays or lists to factory methods (e.g., {@code
 * allOf(validator1, null, validator2)}) will be ignored during processing and lifecycle calls. If
 * the main varargs array or list itself is null, it will be treated as an empty collection.
 */
public final class Validators {

  private Validators() {}

  /**
   * Creates a {@link ValidationProcessor} that evaluates to {@code true} if all of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * false} is returned as soon as the first validator evaluates to {@code false}. If no validators
   * are provided (or all are null), this validator evaluates to {@code true}.
   *
   * @param validators The validators to combine with an AND logic.
   * @return A new {@link ValidationProcessor} representing the AND condition.
   */
  public static ValidationProcessor allOf(ValidationProcessor... validators) {
    return new AllOfValidator(validators);
  }

  /**
   * Creates a {@link ValidationProcessor} that evaluates to {@code true} if all of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * false} is returned as soon as the first validator evaluates to {@code false}. If the list is
   * empty or contains only null validators, this validator evaluates to {@code true}.
   *
   * @param validators A list of validators to combine with an AND logic.
   * @return A new {@link ValidationProcessor} representing the AND condition.
   */
  public static ValidationProcessor allOf(List<ValidationProcessor> validators) {
    return new AllOfValidator(validators);
  }

  /**
   * Creates a {@link ValidationProcessor} that evaluates to {@code true} if any of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * true} is returned as soon as the first validator evaluates to {@code true}. If no validators
   * are provided (or all are null), this validator evaluates to {@code false}.
   *
   * @param validators The validators to combine with an OR logic.
   * @return A new {@link ValidationProcessor} representing the OR condition.
   */
  public static ValidationProcessor anyOf(ValidationProcessor... validators) {
    return new AnyOfValidator(validators);
  }

  /**
   * Creates a {@link ValidationProcessor} that evaluates to {@code true} if any of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * true} is returned as soon as the first validator evaluates to {@code true}. If the list is
   * empty or contains only null validators, this validator evaluates to {@code false}.
   *
   * @param validators A list of validators to combine with an OR logic.
   * @return A new {@link ValidationProcessor} representing the OR condition.
   */
  public static ValidationProcessor anyOf(List<ValidationProcessor> validators) {
    return new AnyOfValidator(validators);
  }

  /**
   * Creates a {@link ValidationProcessor} that evaluates to {@code true} if none of the provided
   * validators evaluate to {@code true} (i.e., all evaluate to {@code false}). This operation
   * short-circuits: evaluation stops and {@code false} is returned as soon as the first validator
   * evaluates to {@code true}. If no validators are provided (or all are null), this validator
   * evaluates to {@code true}.
   *
   * @param validators The validators to combine with a NOR logic.
   * @return A new {@link ValidationProcessor} representing the NOR condition.
   */
  public static ValidationProcessor noneOf(ValidationProcessor... validators) {
    return new NoneOfValidator(validators);
  }

  /**
   * Creates a {@link ValidationProcessor} that evaluates to {@code true} if none of the provided
   * validators evaluate to {@code true} (i.e., all evaluate to {@code false}). This operation
   * short-circuits: evaluation stops and {@code false} is returned as soon as the first validator
   * evaluates to {@code true}. If the list is empty or contains only null validators, this
   * validator evaluates to {@code true}.
   *
   * @param validators A list of validators to combine with a NOR logic.
   * @return A new {@link ValidationProcessor} representing the NOR condition.
   */
  public static ValidationProcessor noneOf(List<ValidationProcessor> validators) {
    return new NoneOfValidator(validators);
  }

  /**
   * Creates a {@link ValidationProcessor} that inverts the result of the given validator. If the
   * provided validator is {@code null}, this method will throw an {@link IllegalArgumentException}.
   *
   * @param validator The validator whose result is to be inverted. Must not be null.
   * @return A new {@link ValidationProcessor} representing the NOT condition.
   * @throws IllegalArgumentException if the provided validator is null.
   */
  public static ValidationProcessor not(ValidationProcessor validator) {
    if (validator == null) {
      throw new IllegalArgumentException("Validator for 'not' operation cannot be null.");
    }
    return new NotValidator(validator);
  }

  /**
   * Returns a {@link ValidationProcessor} that always evaluates to {@code true}. This validator has
   * no side effects during lifecycle calls.
   *
   * @return A singleton instance of a validator that always returns {@code true}.
   */
  public static ValidationProcessor alwaysTrue() {
    return AlwaysTrueValidator.INSTANCE;
  }

  /**
   * Returns a {@link ValidationProcessor} that always evaluates to {@code false}. This validator
   * has no side effects during lifecycle calls.
   *
   * @return A singleton instance of a validator that always returns {@code false}.
   */
  public static ValidationProcessor alwaysFalse() {
    return AlwaysFalseValidator.INSTANCE;
  }

  private static List<ValidationProcessor> copyAndFilterNulls(ValidationProcessor... validators) {
    if (validators == null || validators.length == 0) {
      return Collections.emptyList();
    }
    List<ValidationProcessor> list = new ArrayList<>(validators.length);
    for (ValidationProcessor validator : validators) {
      if (validator != null) {
        list.add(validator);
      }
    }
    return list;
  }

  private static List<ValidationProcessor> copyAndFilterNulls(
      List<ValidationProcessor> validators) {
    if (validators == null || validators.isEmpty()) {
      return Collections.emptyList();
    }
    List<ValidationProcessor> list = new ArrayList<>(validators.size());
    for (ValidationProcessor validator : validators) {
      if (validator != null) {
        list.add(validator);
      }
    }
    return list;
  }

  private abstract static class AbstractCompositeValidator implements ValidationProcessor {
    protected final List<ValidationProcessor> children;

    protected AbstractCompositeValidator(ValidationProcessor... validators) {
      this.children = copyAndFilterNulls(validators);
    }

    protected AbstractCompositeValidator(List<ValidationProcessor> validators) {
      this.children = copyAndFilterNulls(validators);
    }

    @Override
    public void initializeSearch(SearchContext searchContext) {
      for (ValidationProcessor child : children) {
        child.initializeSearch(searchContext);
      }
    }

    @Override
    public void finalizeSearch(SearchContext searchContext) {
      for (ValidationProcessor child : children) {
        child.finalizeSearch(searchContext);
      }
    }
  }

  private static class AllOfValidator extends AbstractCompositeValidator {
    public AllOfValidator(ValidationProcessor... validators) {
      super(validators);
    }

    public AllOfValidator(List<ValidationProcessor> validators) {
      super(validators);
    }

    @Override
    public boolean isValid(EvaluationContext context) {
      for (ValidationProcessor child : children) {
        if (!child.isValid(context)) {
          return false;
        }
      }
      return true;
    }
  }

  private static class AnyOfValidator extends AbstractCompositeValidator {
    public AnyOfValidator(ValidationProcessor... validators) {
      super(validators);
    }

    public AnyOfValidator(List<ValidationProcessor> validators) {
      super(validators);
    }

    @Override
    public boolean isValid(EvaluationContext context) {
      if (children.isEmpty()) {
        return false;
      }
      for (ValidationProcessor child : children) {
        if (child.isValid(context)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class NoneOfValidator extends AbstractCompositeValidator {
    public NoneOfValidator(ValidationProcessor... validators) {
      super(validators);
    }

    public NoneOfValidator(List<ValidationProcessor> validators) {
      super(validators);
    }

    @Override
    public boolean isValid(EvaluationContext context) {
      for (ValidationProcessor child : children) {
        if (child.isValid(context)) {
          return false;
        }
      }
      return true;
    }
  }

  private static class NotValidator implements ValidationProcessor {
    private final ValidationProcessor child;

    public NotValidator(ValidationProcessor validator) {
      this.child =
          Objects.requireNonNull(
              validator, "Child validator for NotValidator cannot be null post-construction.");
    }

    @Override
    public void initializeSearch(SearchContext searchContext) {
      child.initializeSearch(searchContext);
    }

    @Override
    public boolean isValid(EvaluationContext context) {
      return !child.isValid(context);
    }

    @Override
    public void finalizeSearch(SearchContext searchContext) {
      child.finalizeSearch(searchContext);
    }
  }

  private static class AlwaysTrueValidator implements ValidationProcessor {
    public static final AlwaysTrueValidator INSTANCE = new AlwaysTrueValidator();

    private AlwaysTrueValidator() {}

    @Override
    public boolean isValid(EvaluationContext context) {
      return true;
    }
  }

  private static class AlwaysFalseValidator implements ValidationProcessor {
    public static final AlwaysFalseValidator INSTANCE = new AlwaysFalseValidator();

    private AlwaysFalseValidator() {}

    @Override
    public boolean isValid(EvaluationContext context) {
      return false;
    }
  }
}
