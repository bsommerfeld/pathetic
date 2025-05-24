package de.bsommerfeld.pathetic.api.pathing.processing;

import de.bsommerfeld.pathetic.api.pathing.processing.context.NodeEvaluationContext;
import de.bsommerfeld.pathetic.api.pathing.processing.context.SearchContext; // Assuming this exists
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for creating and combining {@link NodeValidationProcessor} instances. This class
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
   * Creates a {@link NodeValidationProcessor} that evaluates to {@code true} if all of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * false} is returned as soon as the first validator evaluates to {@code false}. If no validators
   * are provided (or all are null), this validator evaluates to {@code true}.
   *
   * @param validators The validators to combine with an AND logic.
   * @return A new {@link NodeValidationProcessor} representing the AND condition.
   */
  public static NodeValidationProcessor allOf(NodeValidationProcessor... validators) {
    return new AllOfValidator(validators);
  }

  /**
   * Creates a {@link NodeValidationProcessor} that evaluates to {@code true} if all of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * false} is returned as soon as the first validator evaluates to {@code false}. If the list is
   * empty or contains only null validators, this validator evaluates to {@code true}.
   *
   * @param validators A list of validators to combine with an AND logic.
   * @return A new {@link NodeValidationProcessor} representing the AND condition.
   */
  public static NodeValidationProcessor allOf(List<NodeValidationProcessor> validators) {
    return new AllOfValidator(validators);
  }

  /**
   * Creates a {@link NodeValidationProcessor} that evaluates to {@code true} if any of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * true} is returned as soon as the first validator evaluates to {@code true}. If no validators
   * are provided (or all are null), this validator evaluates to {@code false}.
   *
   * @param validators The validators to combine with an OR logic.
   * @return A new {@link NodeValidationProcessor} representing the OR condition.
   */
  public static NodeValidationProcessor anyOf(NodeValidationProcessor... validators) {
    return new AnyOfValidator(validators);
  }

  /**
   * Creates a {@link NodeValidationProcessor} that evaluates to {@code true} if any of the provided
   * validators evaluate to {@code true}. This operation short-circuits: evaluation stops and {@code
   * true} is returned as soon as the first validator evaluates to {@code true}. If the list is
   * empty or contains only null validators, this validator evaluates to {@code false}.
   *
   * @param validators A list of validators to combine with an OR logic.
   * @return A new {@link NodeValidationProcessor} representing the OR condition.
   */
  public static NodeValidationProcessor anyOf(List<NodeValidationProcessor> validators) {
    return new AnyOfValidator(validators);
  }

  /**
   * Creates a {@link NodeValidationProcessor} that evaluates to {@code true} if none of the
   * provided validators evaluate to {@code true} (i.e., all evaluate to {@code false}). This
   * operation short-circuits: evaluation stops and {@code false} is returned as soon as the first
   * validator evaluates to {@code true}. If no validators are provided (or all are null), this
   * validator evaluates to {@code true}.
   *
   * @param validators The validators to combine with a NOR logic.
   * @return A new {@link NodeValidationProcessor} representing the NOR condition.
   */
  public static NodeValidationProcessor noneOf(NodeValidationProcessor... validators) {
    return new NoneOfValidator(validators);
  }

  /**
   * Creates a {@link NodeValidationProcessor} that evaluates to {@code true} if none of the
   * provided validators evaluate to {@code true} (i.e., all evaluate to {@code false}). This
   * operation short-circuits: evaluation stops and {@code false} is returned as soon as the first
   * validator evaluates to {@code true}. If the list is empty or contains only null validators,
   * this validator evaluates to {@code true}.
   *
   * @param validators A list of validators to combine with a NOR logic.
   * @return A new {@link NodeValidationProcessor} representing the NOR condition.
   */
  public static NodeValidationProcessor noneOf(List<NodeValidationProcessor> validators) {
    return new NoneOfValidator(validators);
  }

  /**
   * Creates a {@link NodeValidationProcessor} that inverts the result of the given validator. If
   * the provided validator is {@code null}, this method will throw an {@link
   * IllegalArgumentException}.
   *
   * @param validator The validator whose result is to be inverted. Must not be null.
   * @return A new {@link NodeValidationProcessor} representing the NOT condition.
   * @throws IllegalArgumentException if the provided validator is null.
   */
  public static NodeValidationProcessor not(NodeValidationProcessor validator) {
    if (validator == null) {
      throw new IllegalArgumentException("Validator for 'not' operation cannot be null.");
    }
    return new NotValidator(validator);
  }

  /**
   * Returns a {@link NodeValidationProcessor} that always evaluates to {@code true}. This validator
   * has no side effects during lifecycle calls.
   *
   * @return A singleton instance of a validator that always returns {@code true}.
   */
  public static NodeValidationProcessor alwaysTrue() {
    return AlwaysTrueValidator.INSTANCE;
  }

  /**
   * Returns a {@link NodeValidationProcessor} that always evaluates to {@code false}. This
   * validator has no side effects during lifecycle calls.
   *
   * @return A singleton instance of a validator that always returns {@code false}.
   */
  public static NodeValidationProcessor alwaysFalse() {
    return AlwaysFalseValidator.INSTANCE;
  }

  private static List<NodeValidationProcessor> copyAndFilterNulls(
      NodeValidationProcessor... validators) {
    if (validators == null || validators.length == 0) {
      return Collections.emptyList();
    }
    List<NodeValidationProcessor> list = new ArrayList<>(validators.length);
    for (NodeValidationProcessor validator : validators) {
      if (validator != null) {
        list.add(validator);
      }
    }
    return list;
  }

  private static List<NodeValidationProcessor> copyAndFilterNulls(
      List<NodeValidationProcessor> validators) {
    if (validators == null || validators.isEmpty()) {
      return Collections.emptyList();
    }
    List<NodeValidationProcessor> list = new ArrayList<>(validators.size());
    for (NodeValidationProcessor validator : validators) {
      if (validator != null) {
        list.add(validator);
      }
    }
    return list;
  }

  private abstract static class AbstractCompositeValidator implements NodeValidationProcessor {
    protected final List<NodeValidationProcessor> children;

    protected AbstractCompositeValidator(NodeValidationProcessor... validators) {
      this.children = copyAndFilterNulls(validators);
    }

    protected AbstractCompositeValidator(List<NodeValidationProcessor> validators) {
      this.children = copyAndFilterNulls(validators);
    }

    @Override
    public void initializeSearch(SearchContext searchContext) {
      for (NodeValidationProcessor child : children) {
        child.initializeSearch(searchContext);
      }
    }

    @Override
    public void finalizeSearch(SearchContext searchContext) {
      for (NodeValidationProcessor child : children) {
        child.finalizeSearch(searchContext);
      }
    }
  }

  private static class AllOfValidator extends AbstractCompositeValidator {
    public AllOfValidator(NodeValidationProcessor... validators) {
      super(validators);
    }

    public AllOfValidator(List<NodeValidationProcessor> validators) {
      super(validators);
    }

    @Override
    public boolean isValid(NodeEvaluationContext context) {
      for (NodeValidationProcessor child : children) {
        if (!child.isValid(context)) {
          return false;
        }
      }
      return true;
    }
  }

  private static class AnyOfValidator extends AbstractCompositeValidator {
    public AnyOfValidator(NodeValidationProcessor... validators) {
      super(validators);
    }

    public AnyOfValidator(List<NodeValidationProcessor> validators) {
      super(validators);
    }

    @Override
    public boolean isValid(NodeEvaluationContext context) {
      if (children.isEmpty()) {
        return false;
      }
      for (NodeValidationProcessor child : children) {
        if (child.isValid(context)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class NoneOfValidator extends AbstractCompositeValidator {
    public NoneOfValidator(NodeValidationProcessor... validators) {
      super(validators);
    }

    public NoneOfValidator(List<NodeValidationProcessor> validators) {
      super(validators);
    }

    @Override
    public boolean isValid(NodeEvaluationContext context) {
      for (NodeValidationProcessor child : children) {
        if (child.isValid(context)) {
          return false;
        }
      }
      return true;
    }
  }

  private static class NotValidator implements NodeValidationProcessor {
    private final NodeValidationProcessor child;

    public NotValidator(NodeValidationProcessor validator) {
      this.child =
          Objects.requireNonNull(
              validator, "Child validator for NotValidator cannot be null post-construction.");
    }

    @Override
    public void initializeSearch(SearchContext searchContext) {
      child.initializeSearch(searchContext);
    }

    @Override
    public boolean isValid(NodeEvaluationContext context) {
      return !child.isValid(context);
    }

    @Override
    public void finalizeSearch(SearchContext searchContext) {
      child.finalizeSearch(searchContext);
    }
  }

  private static class AlwaysTrueValidator implements NodeValidationProcessor {
    public static final AlwaysTrueValidator INSTANCE = new AlwaysTrueValidator();

    private AlwaysTrueValidator() {}

    @Override
    public boolean isValid(NodeEvaluationContext context) {
      return true;
    }
  }

  private static class AlwaysFalseValidator implements NodeValidationProcessor {
    public static final AlwaysFalseValidator INSTANCE = new AlwaysFalseValidator();

    private AlwaysFalseValidator() {}

    @Override
    public boolean isValid(NodeEvaluationContext context) {
      return false;
    }
  }
}
