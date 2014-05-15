package org._3pq.jgrapht.util;

public class RedundancyStats {
    private int m_redundant = 0;
    private int m_total     = 0;

    public RedundancyStats(  ) {}

    public void update( int weight, boolean redundant ) {
        if (redundant)
            m_redundant += weight;
        m_total += weight;
    }

    public int getRedundant(  ) {
        return m_redundant;
    }

    public int getTotal(  ) {
        return m_total;
    }

    public float getRatio(  ) {
        return (m_total == 0 ? 0 : (float) m_redundant / (float) m_total);
    }
}
