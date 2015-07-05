package com.jonoler.longpowerset;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class LongPowerSet {
  public static <E> LongSet<Set<E>> create(Set<E> elements) {
    return new LongPowerSetImpl<>(elements);
  }

  private static final class LongPowerSetImpl<E> extends AbstractSet<Set<E>> implements LongSet<Set<E>> {
    private static final int MAX_ELEMENTS = 63;

    private Map<E, Integer> elementPositions;
    private E[] elementArray;

    LongPowerSetImpl(Set<E> elements) {
      if (elements.size() > MAX_ELEMENTS) {
        throw new IllegalArgumentException("Max elements of " + MAX_ELEMENTS + " exceeded: " + elements.size());
      }
      elementPositions = new HashMap<>(elements.size());
      elementArray = (E[])new Object[elements.size()];
      int index = 0;
      for (E element : elements) {
        elementPositions.put(element, index);
        elementArray[index] = element;
        index++;
      }
    }

    @Override
    public Iterator<Set<E>> iterator() {
      return new ConsecutiveLongMaskSetIterator<>(elementPositions, elementArray);
    }

    @Override
    public int size() {
      throw new UnsupportedOperationException("Use longSize() instead.");
    }

    @Override
    public long longSize() {
      return 1l << elementArray.length;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public boolean contains(Object obj) {
      if (obj instanceof Collection) {
        Collection<?> otherCollection = (Collection<?>)obj;
        return elementPositions.keySet().containsAll(otherCollection);
      }
      return false;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof LongPowerSetImpl) {
        LongPowerSetImpl<?> otherPowerSet = (LongPowerSetImpl<?>)obj;
        return Arrays.equals(elementArray, otherPowerSet.elementArray);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return ((Long)(((long)Arrays.asList(elementArray).hashCode()) << elementArray.length - 1)).hashCode();
    }
  }

  private static final class ConsecutiveLongMaskSetIterator<E> implements Iterator<Set<E>> {
    private Map<E, Integer> elementPositions;
    private E[] elementArray;
    private long currentPowerSetElementMask;
    private long finalPowerSetElementMask;

    ConsecutiveLongMaskSetIterator(Map<E, Integer> elementPositions, E[] elementArray) {
      this.elementPositions = elementPositions;
      this.elementArray = elementArray;
      finalPowerSetElementMask = elementArray.length == LongPowerSetImpl.MAX_ELEMENTS ?
          Long.MAX_VALUE : 1l << elementArray.length;
    }

    @Override
    public boolean hasNext() {
      return currentPowerSetElementMask < finalPowerSetElementMask;
    }

    @Override
    public Set<E> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return new LongMaskSet<>(elementPositions, elementArray, currentPowerSetElementMask++);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  static final class LongMaskSet<E> extends AbstractSet<E> {
    private Map<E, Integer> elementPositions;
    private E[] elementArray;
    private long mask;

    LongMaskSet(Map<E, Integer> elementPositions, E[] elementArray, long mask) {
      this.elementPositions = elementPositions;
      this.elementArray = elementArray;
      this.mask = mask;
    }

    @Override
    public int size() {
      return Long.bitCount(mask);
    }

    @Override
    public boolean contains(Object obj) {
      Integer index = elementPositions.get(obj);
      return index != null && (mask & (1l << index)) != 0;
    }

    @Override
    public Iterator<E> iterator() {
      return new Iterator<E>() {
        private long currentMask = mask;

        @Override
        public boolean hasNext() {
          return currentMask != 0;
        }

        @Override
        public E next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          int elementIndex = Long.numberOfTrailingZeros(currentMask);
          currentMask &= ~(1l << elementIndex);
          return elementArray[elementIndex];
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
}
