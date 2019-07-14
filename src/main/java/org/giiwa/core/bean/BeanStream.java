/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.core.bean;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper.Cursor;

/**
 * The {@code BeanStream} Class used to contains the Bean as cursor. <br>
 * MUSt close it after get data
 * 
 */
public final class BeanStream<E extends Bean> implements Stream<E> {

	/** The log. */
	protected static Log log = LogFactory.getLog(BeanStream.class);

	Cursor<E> cur;

	public static <E extends Bean> BeanStream<E> create(Cursor<E> cur) {
		return new BeanStream<E>();
	}

	@Override
	public Iterator<E> iterator() {
		return cur;
	}

	@Override
	public Spliterator<E> spliterator() {

		return null;
	}

	@Override
	public boolean isParallel() {
		return true;
	}

	@Override
	public Stream<E> sequential() {
		return null;
	}

	@Override
	public Stream<E> parallel() {
		return null;
	}

	@Override
	public Stream<E> unordered() {
		return null;
	}

	@Override
	public Stream<E> onClose(Runnable closeHandler) {
		return null;
	}

	@Override
	public void close() {
		cur.close();
	}

	@Override
	public Stream<E> filter(Predicate<? super E> predicate) {
		return null;
	}

	@Override
	public <R> Stream<R> map(Function<? super E, ? extends R> mapper) {
		return null;
	}

	@Override
	public IntStream mapToInt(ToIntFunction<? super E> mapper) {
		return null;
	}

	@Override
	public LongStream mapToLong(ToLongFunction<? super E> mapper) {
		return null;
	}

	@Override
	public DoubleStream mapToDouble(ToDoubleFunction<? super E> mapper) {
		return null;
	}

	@Override
	public <R> Stream<R> flatMap(Function<? super E, ? extends Stream<? extends R>> mapper) {
		return null;
	}

	@Override
	public IntStream flatMapToInt(Function<? super E, ? extends IntStream> mapper) {
		return null;
	}

	@Override
	public LongStream flatMapToLong(Function<? super E, ? extends LongStream> mapper) {
		return null;
	}

	@Override
	public DoubleStream flatMapToDouble(Function<? super E, ? extends DoubleStream> mapper) {
		return null;
	}

	@Override
	public Stream<E> distinct() {
		return null;
	}

	@Override
	public Stream<E> sorted() {
		return null;
	}

	@Override
	public Stream<E> sorted(Comparator<? super E> comparator) {
		return null;
	}

	@Override
	public Stream<E> peek(Consumer<? super E> action) {
		return null;
	}

	@Override
	public Stream<E> limit(long maxSize) {
		return null;
	}

	@Override
	public Stream<E> skip(long n) {
		return null;
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		while (cur.hasNext()) {
			action.accept(cur.next());
		}
	}

	@Override
	public void forEachOrdered(Consumer<? super E> action) {

	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		return null;
	}

	@Override
	public E reduce(E identity, BinaryOperator<E> accumulator) {
		return null;
	}

	@Override
	public Optional<E> reduce(BinaryOperator<E> accumulator) {
		return Optional.empty();
	}

	@Override
	public <U> U reduce(U identity, BiFunction<U, ? super E, U> accumulator, BinaryOperator<U> combiner) {
		return null;
	}

	@Override
	public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super E> accumulator, BiConsumer<R, R> combiner) {
		return null;
	}

	@Override
	public <R, A> R collect(Collector<? super E, A, R> collector) {
		return null;
	}

	@Override
	public Optional<E> min(Comparator<? super E> comparator) {
		return Optional.empty();
	}

	@Override
	public Optional<E> max(Comparator<? super E> comparator) {
		return Optional.empty();
	}

	@Override
	public long count() {
		return -1;
	}

	@Override
	public boolean anyMatch(Predicate<? super E> predicate) {
		return false;
	}

	@Override
	public boolean allMatch(Predicate<? super E> predicate) {
		return false;
	}

	@Override
	public boolean noneMatch(Predicate<? super E> predicate) {
		return false;
	}

	@Override
	public Optional<E> findFirst() {
		return Optional.empty();
	}

	@Override
	public Optional<E> findAny() {
		return Optional.empty();
	}

}
