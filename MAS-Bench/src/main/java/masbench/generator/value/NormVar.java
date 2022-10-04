package masbench.generator.value;

public class NormVar {
    public Double sigma;
    public Double mu;
    public Double pi;

    public NormVar(Double sigma, Double mu, Double pi) {
        this.sigma = sigma;
        this.mu = mu;
        this.pi = pi;
    }

    public NormVar() {
        this(0.0,0.0,0.0);
    }

    public String toString() {
        return "s:" + sigma + ", m:" + mu + ", pi:" + pi;
    }
}
