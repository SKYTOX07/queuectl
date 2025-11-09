
package com.queuectl.domain;

import jakarta.persistence.*;

@Entity
@Table(name="kv_config")
public class KeyValueConfig {
    @Id
    @Column(name = "k", length = 120)
    private String k;

    @Column(name = "v", length = 2000)
    private String v;

    public KeyValueConfig() {}
    public KeyValueConfig(String k, String v) { this.k = k; this.v = v; }

    public String getK() { return k; }
    public void setK(String k) { this.k = k; }
    public String getV() { return v; }
    public void setV(String v) { this.v = v; }
}
