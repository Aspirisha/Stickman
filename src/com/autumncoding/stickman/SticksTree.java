package com.autumncoding.stickman;

public class SticksTree {
	SticksTree parent;
	Stick m_stick;
	int rank;
	
	public SticksTree() {
		parent = this;
		rank = 0;
	}
	
	public static void Union(SticksTree x, SticksTree y) {
		Link(FindSet(x), FindSet(y));
	}
	
	public static void Link(SticksTree x, SticksTree y) {
		if (x.rank < y.rank)
			x.parent = y.parent;
		else {
			y.parent = x.parent;
			if (x.rank == y.rank)
				x.rank++;
		}
	}
	
	public static SticksTree FindSet(SticksTree x) {
		if (x.parent != x)
			x.parent = FindSet(x.parent);
		
		return x.parent;
	}
}
