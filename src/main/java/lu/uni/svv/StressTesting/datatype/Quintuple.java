package lu.uni.svv.StressTesting.datatype;

public class Quintuple<A, B, C, D, E> {
	
	public final A first;
	public final B second;
	public final C third;
	public final D fourth;
	public final E fifth;
	
	public Quintuple(A first, B second, C third, D fourth, E fifth) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.fourth = fourth;
		this.fifth = fifth;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Quintuple)) {
			return false;
		}
		Quintuple<?, ?, ?, ?, ?> p = (Quintuple<?, ?, ?, ?, ?>) o;
		return first.equals(p.first) && second.equals(p.second) && third.equals(p.third) && fourth.equals(p.fourth) && fifth.equals(p.fifth);
	}
	
	
	@Override
	public int hashCode() {
		return (first == null ? 0 : first.hashCode())
				^ (second == null ? 0 : second.hashCode())
				^ (third == null ? 0 : third.hashCode())
				^ (fourth == null ? 0 : fourth.hashCode())
				^ (fifth == null ? 0 : fifth.hashCode());
	}
	
	public static boolean equals(Object x, Object y) {
		return (x == null && y == null) || (x != null && x.equals(y));
	}
	
	public static <A, B, C, D, E> Quintuple <A, B, C, D, E> create(A a, B b, C c, D d, E e) {
		return new Quintuple<A, B, C, D, E>(a, b, c, d, e);
	}
}