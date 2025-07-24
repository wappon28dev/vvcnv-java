package wappon28dev.vvcnv_java.util;

import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Java implementation of Rust's Result type using sealed classes (Java 21
 * feature)
 * 
 * @param <T> The success type
 * @param <E> The error type
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

  record Ok<T, E>(T value) implements Result<T, E> {
  }

  record Err<T, E>(E error) implements Result<T, E> {
  }

  static <T, E> Result<T, E> ok(T value) {
    return new Ok<>(value);
  }

  static <T, E> Result<T, E> err(E error) {
    return new Err<>(error);
  }

  default boolean isOk() {
    return this instanceof Ok;
  }

  default boolean isErr() {
    return this instanceof Err;
  }

  default T unwrap() {
    return switch (this) {
      case Ok<T, E>(var value) -> value;
      case Err<T, E>(var error) -> throw new RuntimeException("Unwrapped an Err: " + error);
    };
  }

  default T unwrapOr(T defaultValue) {
    return switch (this) {
      case Ok<T, E>(var value) -> value;
      case Err<T, E>(var error) -> defaultValue;
    };
  }

  default T unwrapOrElse(Supplier<T> supplier) {
    return switch (this) {
      case Ok<T, E>(var value) -> value;
      case Err<T, E>(var error) -> supplier.get();
    };
  }

  default <U> Result<U, E> map(Function<T, U> mapper) {
    return switch (this) {
      case Ok<T, E>(var value) -> new Ok<>(mapper.apply(value));
      case Err<T, E>(var error) -> new Err<>(error);
    };
  }

  default <F> Result<T, F> mapErr(Function<E, F> mapper) {
    return switch (this) {
      case Ok<T, E>(var value) -> new Ok<>(value);
      case Err<T, E>(var error) -> new Err<>(mapper.apply(error));
    };
  }

  default <U> Result<U, E> andThen(Function<T, Result<U, E>> mapper) {
    return switch (this) {
      case Ok<T, E>(var value) -> mapper.apply(value);
      case Err<T, E>(var error) -> new Err<>(error);
    };
  }

  default Result<T, E> inspect(Consumer<T> consumer) {
    if (this instanceof Ok<T, E>(var value)) {
      consumer.accept(value);
    }
    return this;
  }

  default Result<T, E> inspectErr(Consumer<E> consumer) {
    if (this instanceof Err<T, E>(var error)) {
      consumer.accept(error);
    }
    return this;
  }
}
