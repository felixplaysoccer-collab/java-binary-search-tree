package edu.uwm.cs351.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

public class TreeMap<K,V>  extends AbstractMap<K,V> {

	// Here is the data structure to use
	// By extending ``DefaultEntry'' each node can be seen as an entry
	// which makes the entry set iterator much easier than it may appear.
	
	private static class Node<K,V> extends DefaultEntry<K,V> {
		Node<K,V> left, right;
		Node<K,V> parent;
		Node(K k, V v) {
			super(k,v);
			parent = left = right = null;
		}
	}
	
	private Comparator<K> comparator;
	private Node<K,V> dummy;
	private int numItems = 0;
	private int version = 0;
	
	
	/// Invariant checks:
	
	private static Consumer<String> reporter = (s) -> { System.err.println("Invariant error: " + s); };
	
	private boolean report(String error) {
		reporter.accept(error);
		return false;
	}
	
	/**
	 * Return whether nodes in the subtree rooted at the given node have correct parent
	 * and have keys that are never null and are correctly sorted and are all in the range 
	 * between the lower and upper (both exclusive).
	 * If either bound is null, then that means that there is no limit at this side.
	 * The first problem is found will be reported.
	 * @param node root of subtree to examine
	 * @param p parent of subtree to examine
	 * @param lower value that all nodes must be greater than.  If null, then
	 * there is no lower bound.
	 * @param upper value that all nodes must be less than. If null,
	 * then there is no upper bound.
	 * @return whether the subtree is fine.  If false is 
	 * returned, there is a problem, which has already been reported.
	 */
	private boolean checkInRange(Node<K,V> node, Node<K, V> p, K lower, K upper) {
		if (node == null) return true;

		if (node.parent != p) return report("wrong parent pointer");
		if (node.key == null) return report("non-dummy node has null key");

		if (lower != null && comparator.compare(node.key, lower) <= 0) {
			return report("key is not greater than lower bound");
		}
		if (upper != null && comparator.compare(node.key, upper) >= 0) {
			return report("key is not less than upper bound");
		}

		return checkInRange(node.left, node, lower, node.key) &&
			   checkInRange(node.right, node, node.key, upper);
	 // TODO
	}
	
	/**
	 * Return the number of nodes in a binary tree.
	 * @param r binary (search) tree, may be null but must not have cycles
	 * @return number of nodes in this tree
	 */
	private int countNodes(Node<K,V> r) {
		if (r == null) return 0;
		return 1 + countNodes(r.left) + countNodes(r.right);
	}
		
	/**
	 * Check the invariant, printing a message if not satisfied.
	 * @return whether invariant is correct
	 */
	private boolean wellFormed() {
		if (comparator == null) return report("null comparator");
		if (dummy == null) return report("null dummy node");

		if (dummy.key != null) return report("dummy must have null key");
		if (dummy.parent != null) return report("dummy must have null parent");
		if (dummy.right != null) return report("dummy must have null right child");

		if (!checkInRange(dummy.left, dummy, null, null)) return false;

		if (countNodes(dummy.left) != numItems) {
			return report("wrong numItems");
		}
		// TODO:
		// 1. check that comparator is not null
		// 2. check that dummy is not null
		// 3. check that dummy's key, right subtree and parent are null
		// 4. check that all (non-dummy) nodes are in range
		// 5. check that all nodes have correct parents
		// 6. check that number of items matches number of (non-dummy) nodes
		// "checkInRange" will help with 4,5
		return true;
	}
	
	@Override // efficiency
	public boolean containsKey(Object o) {
		assert wellFormed() : "invariant broken at start of containsKey";
		return findKey(o) != null;
	}
	
	@Override // efficiency
	public void clear() {
		assert wellFormed() : "invariant broken at start of clear";
		if (numItems > 0) {
			dummy.left = null;
			numItems = 0;
			++version;
		}
		assert wellFormed() : "invariant broken at end of clear";
	}
	/// constructors
	
	private TreeMap(boolean ignored) { } // do not change this.
	
	/**
	 * Create an empty TreeMap with the default comparator.
	 */
	public TreeMap() {
		this(null);
		assert wellFormed() : "invariant broken after constructor()";
	}
	
	/**
	 * Create an empty tree map with the given comparator, or the default
	 * comparator if the argument is null
	 * @param c comparator to use, if null use default comparator
	 */
	public TreeMap(Comparator<K> c) {
		if (c == null) {
			comparator = (Comparator<K>) new Comparator<Comparable<Object>>() {
				@Override
				public int compare(Comparable<Object> o1, Comparable<Object> o2) {
					return o1.compareTo(o2);
				}
			};
		} else {
			comparator = c;
		}
		dummy = new Node<K,V>(null, null);
		assert wellFormed() : "invariant broken after constructor(Comparator)";
	}
	
	
	@Override // implementation
	public V get(Object key) {
		assert wellFormed() : "invariant broken at start of get";
		Node<K,V> n = findKey(key);
		return n == null ? null : n.value;
	}
	
	@Override // implementation
	public int size() {
		assert wellFormed() : "invariant broken at start of size";
		return numItems;
	}// TODO
		// Update the parameter comparator if necessary
		// Create the dummy node.
		
	
	public V put(K key, V value) {
		assert wellFormed() : "invariant broken at start of put";
		if (key == null) throw new NullPointerException("key must not be null");

		if (dummy.left == null) {
			dummy.left = new Node<K,V>(key, value);
			dummy.left.parent = dummy;
			++numItems;
			++version;
			assert wellFormed() : "invariant broken at end of put";
			return null;
		}

		Node<K,V> p = dummy.left;
		for (;;) {
			int c = comparator.compare(key, p.key);
			if (c == 0) {
				V old = p.value;
				p.value = value;
				assert wellFormed() : "invariant broken at end of put";
				return old;
			} else if (c < 0) {
				if (p.left == null) {
					p.left = new Node<K,V>(key, value);
					p.left.parent = p;
					++numItems;
					++version;
					assert wellFormed() : "invariant broken at end of put";
					return null;
				}
				p = p.left;
			} else {
				if (p.right == null) {
					p.right = new Node<K,V>(key, value);
					p.right.parent = p;
					++numItems;
					++version;
					assert wellFormed() : "invariant broken at end of put";
					return null;
				}
				p = p.right;
			}
		}
	}
	

	@SuppressWarnings("unchecked")
	private K asKey(Object x) {
		if (dummy.left == null || x == null) return null;
		try {
			comparator.compare(dummy.left.key,(K)x);
			comparator.compare((K)x,dummy.left.key);
			return (K)x;
		} catch (ClassCastException ex) {
			return null;
		}
	}

	/**
	 * Find the node for a given key.  Return null if the key isn't present
	 * in the tree.  This helper method assumes that the tree is well formed,
	 * but doesn't check that.
	 * @param o object treated as a key.
	 * @return node whose data is equal to o, 
	 * or null if no nodes in the tree have this property.
	 */
	
	private Node<K, V> findKey(Object o){
		K key = asKey(o);
		if (key == null) return null;
		
		Node<K,V> p = dummy.left;
		while (p != null) {
			int c = comparator.compare(key, p.key);
			if (c == 0) return p;
			if (c < 0) p = p.left;
			else p = p.right;
		}
		
		return null; // TODO (non-recursive is fine)
	}
	
	private void doRemove(Node<K,V> r) {
		if (r.left != null && r.right != null) {
			Node<K,V> s = r.right;
			while (s.left != null) {
				s = s.left;
			}
			r.key = s.key;
			r.value = s.value;
			r = s;
		}

		Node<K,V> child = (r.left != null) ? r.left : r.right;
		Node<K,V> p = r.parent;

		if (child != null) {
			child.parent = p;
		}

		if (p.left == r) {
			p.left = child;
		} else {
			p.right = child;
		}

		--numItems;
		++version;
	}
	
	@Override // implementation
	public V remove(Object o) {
		assert wellFormed() : "invariant broken at start of remove";
		Node<K,V> n = findKey(o);
		if (n == null) return null;

		V old = n.value;
		doRemove(n);

		assert wellFormed() : "invariant broken at end of remove";
		return old;
	}

	// TODO: many methods to override here.
	// Override as needed. and document why:
	// required -- or else there is a compiler error
	// implementation -- or else a normal test fails
	// efficiency -- or else an efficiency test fails
	// Our solution has a complex helper method for removal.
	// make sure to assert wellformedness in any public method.

	
	private volatile Set<Entry<K,V>> entrySet;
	
	@Override // required
	public Set<Entry<K, V>> entrySet() {
		assert wellFormed() : "invariant broken at beginning of entrySet";
		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	/**
	 * The set for this map, backed by the map.
	 * By "backed: we mean that this set doesn't have its own data structure:
	 * it uses the data structure of the map.
	 */
	private class EntrySet extends AbstractSet<Entry<K,V>> {
		// Do NOT add any fields! 
		
		@Override // required
		public int size() {
			return TreeMap.this.size();
		 	// TODO
		}

		@Override // required
		public Iterator<Entry<K, V>> iterator() {
			return new MyIterator();
		}
		@Override // implementation
		public boolean remove(Object x) {
			if (!(x instanceof Entry<?,?>)) return false;
			Entry<?,?> e = (Entry<?,?>)x;
			Node<K,V> n = findKey(e.getKey());
			if (n == null) return false;
			if (n.getValue() == null ? e.getValue() != null : !n.getValue().equals(e.getValue())) {
				return false;
			}
			TreeMap.this.remove(n.key);
			return true;
		}
		@Override // efficiency
		public boolean contains(Object x) {
			if (!(x instanceof Entry<?,?>)) return false;
			Entry<?,?> e = (Entry<?,?>)x;
			Node<K,V> n = findKey(e.getKey());
			if (n == null) return false;
			Object v = e.getValue();
			return n.value == null ? v == null : n.value.equals(v);
		}
		// TODO other overrides as needed
	}

	
	/**
	 * Iterator over the map.
	 * We use parent pointers.
	 * current points to node (if any) that can be removed.
	 * next points to dummy indicating no more next.
	 */
	private class MyIterator implements Iterator<Entry<K,V>> { 
		
		Node<K, V> current, next;
		int colVersion = version;
		
		boolean wellFormed() {
			if (!TreeMap.this.wellFormed()) return false;
			if (colVersion != version) return true;

			if (next == null) return report("next is null");
			if (current == dummy) return report("current is dummy");

			if (next != dummy && findKey(next.key) != next) return report("next not in tree");
			if (current != null && findKey(current.key) != current) return report("current not in tree");

			Node<K,V> pred;
			if (next == dummy) {
				pred = dummy.left;
				if (pred != null) {
					while (pred.right != null) pred = pred.right;
				}
			} else if (next.left != null) {
				pred = next.left;
				while (pred.right != null) pred = pred.right;
			} else {
				Node<K,V> p = dummy.left;
				pred = null;
				while (p != null && p != next) {
					int c = comparator.compare(next.key, p.key);
					if (c < 0) {
						p = p.left;
					} else if (c > 0) {
						pred = p;
						p = p.right;
					} else {
						break;
					}
				}
				if (p != next) return report("next not reachable");
			}

			if (current != null && current != pred) {
				return report("current is not right before next");
			}
			return true;
		}
		
		MyIterator(boolean ignored) {} // do not change this
		
		MyIterator() {
			current = null;
			next = dummy;
			if (dummy.left != null) {
				next = dummy.left;
				while (next.left != null) next = next.left;
			}
			assert wellFormed() : "invariant broken after iterator constructor";
		}
		
		public void checkVersion() {
			if (version != colVersion) throw new ConcurrentModificationException("stale iterator");
		}
		
		@Override // implementation
		public boolean hasNext() {
			assert wellFormed() : "invariant broken before hasNext()";
			checkVersion();
			return next != dummy;
		}

		@Override // implementation
		public Entry<K, V> next() {
			assert wellFormed() : "invariant broken at start of next()";
			checkVersion();
			if (next == dummy) throw new NoSuchElementException();

			current = next;

			if (next.right != null) {
				next = next.right;
				while (next.left != null) next = next.left;
			} else {
				Node<K,V> child = next;
				next = next.parent;
				while (next != dummy && child == next.right) {
					child = next;
					next = next.parent;
				}
			}

			assert wellFormed() : "invariant broken at end of next()";
			return current;
		}

		@Override // implementation
		public void remove() {
			assert wellFormed() : "invariant broken at start of iterator.remove()";
			checkVersion();
			if (current == null) throw new IllegalStateException();

			Node<K,V> r = current;
			if (r.left != null && r.right != null) {
				next = r;
			}
			TreeMap.this.doRemove(r);
			current = null;
			colVersion = version;

			assert wellFormed() : "invariant broken at end of iterator.remove()";
		}
	}
	
	
	
	/// Junit test support for private internal structure.
	// Do not change this nested class.

	/**
	 * A class for testing the internals of a TreeMap.
	 * It provides access to the private fields and methods of TreeMap
	 * and its private inner classes.
	 */
	public static class Spy {

		/**
		 * Return the current reporter for invariant error messages.
		 * @return current reporter, possibly null
		 */
		public Consumer<String> getReporter() {
			return reporter;
		}

		/**
		 * Set the reporter for invariant error messages.
		 * @param r reporter to use, or null to use the default (stderr)
		 */
		public void setReporter(Consumer<String> r) {
			reporter = r;
		}

		/**
		 * A public subclass of the private Node class, for use in testing.
		 * It exposes setters for the left, right, and parent fields.
		 */
		public static class Node<K,V> extends TreeMap.Node<K,V> {
			/**
			 * Create a node with the given key and value and no children or parent.
			 * @param k key, may be null (for a dummy node)
			 * @param v value
			 */
			public Node(K k, V v) {
				super(k, v);
			}
			/**
			 * Set the left child of this node.
			 * @param n new left child, may be null
			 */
			public void setLeft(Node<K,V> n) { this.left = n; }
			/**
			 * Set the right child of this node.
			 * @param n new right child, may be null
			 */
			public void setRight(Node<K,V> n) { this.right = n; }
			/**
			 * Set the parent of this node.
			 * @param n new parent, may be null
			 */
			public void setParent(Node<K,V> n) { this.parent = n; }
		}

		/**
		 * Create a TreeMap instance with the given fields, bypassing the normal
		 * constructor. The result may not satisfy the invariant.
		 * @param c comparator to use, may be null
		 * @param dummy dummy node to use, may be null
		 * @param numItems value to store in the numItems field
		 * @return new TreeMap with these fields set directly
		 */
		public <K,V> TreeMap<K,V> newInstance(Comparator<K> c, Node<K,V> dummy, int numItems) {
			TreeMap<K,V> result = new TreeMap<>(false);
			result.comparator = c;
			result.dummy = dummy;
			result.numItems = numItems;
			return result;
		}

		/**
		 * Check the invariant of a TreeMap.
		 * @param map TreeMap to check
		 * @return whether the invariant holds; if not, an error has been reported
		 */
		public <K,V> boolean wellFormed(TreeMap<K,V> map) {
			return map.wellFormed();
		}

		/**
		 * Call the private findKey method on a TreeMap.
		 * @param map TreeMap to search
		 * @param key object to look up (may be wrong type or null)
		 * @return the node whose key matches, or null if none
		 */
		public <K,V> Node<K,V> findKey(TreeMap<K,V> map, Object key) {
			return (Node<K,V>) map.findKey(key);
		}

		/**
		 * Create an iterator for a TreeMap with the given fields set directly,
		 * with colVersion set to match the map's current version.
		 * The result may not satisfy the iterator invariant.
		 * @param map the map this iterator belongs to
		 * @param current the current field (last node returned by next(), or null)
		 * @param next the next field (next node to return, or dummy for end-of-iteration)
		 * @return new iterator with these fields set
		 */
		public <K,V> Iterator<Entry<K,V>> newIterator(TreeMap<K,V> map, Node<K,V> current, Node<K,V> next) {
			TreeMap<K,V>.MyIterator it = map.new MyIterator(false);
			it.current = current;
			it.next = next;
			it.colVersion = map.version;
			return it;
		}

		/**
		 * Create an iterator for a TreeMap with the given fields set directly,
		 * including an explicit colVersion (which may differ from the map's version).
		 * The result may not satisfy the iterator invariant.
		 * @param map the map this iterator belongs to
		 * @param current the current field (last node returned by next(), or null)
		 * @param next the next field (next node to return, or dummy for end-of-iteration)
		 * @param colVersion version stamp to store in the iterator
		 * @return new iterator with these fields set
		 */
		public <K,V> Iterator<Entry<K,V>> newIterator(TreeMap<K,V> map, Node<K,V> current, Node<K,V> next, int colVersion) {
			TreeMap<K,V>.MyIterator it = map.new MyIterator(false);
			it.current = current;
			it.next = next;
			it.colVersion = colVersion;
			return it;
		}

		/**
		 * Check the invariant of an iterator created by {@link #newIterator}.
		 * @param it iterator to check (must have been created by newIterator)
		 * @return whether the iterator invariant holds; if not, an error has been reported
		 */
		public boolean wellFormed(Iterator<?> it) {
			return ((TreeMap<?,?>.MyIterator) it).wellFormed();
		}
	}
}
