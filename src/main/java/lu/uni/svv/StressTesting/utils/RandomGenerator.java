package lu.uni.svv.StressTesting.utils;

import java.util.Random;

public class RandomGenerator extends Random {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3933147870525869710L;

	public RandomGenerator()
	{
		super();
	}
	
	/**
	 * This function returns the random number between _start and _end including its border number.
	 * For example, if you give 10 and 20 as parameters respectively for _start and _end,
	 * this function returns one number of [10, 20].
	 * @param _start
	 * @param _end
	 * @return one number of [_start, _end] including _start and _end
	 */
	public int nextInt(int _start, int _end) {
		int bound = _end - _start + 1;
		int value = this.nextInt();
		if (value < 0)
			value = value *-1;
		return (value % bound) + _start;
	}
	
	/**
	 * This function returns the random number between _start and _end including its border number.
	 * For example, if you give 10 and 20 as parameters respectively for _start and _end,
	 * this function returns one number of [10, 20].
	 * @param _start
	 * @param _end
	 * @return one number of [_start, _end] including _start and _end
	 */
	public long nextLong(long _start, long _end) {
		long bound = _end - _start + 1;
		long value = this.nextLong();
		if (value < 0)
			value = value *-1L;
		return (value % bound) + _start;
	}
}
