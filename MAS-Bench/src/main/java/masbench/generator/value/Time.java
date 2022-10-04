package masbench.generator.value;

public class Time {
  public Integer h;
  public Integer m;
  public Integer s;
  public Integer c;

  public Time(Integer h, Integer m, Integer s, Integer c) {
    this.h = h;
    this.m = m;
    this.s = s;
    this.c = c;
  }

  public Time step() {
    c += 1;
    h = 19 + (c / 60);
    m = c % 60;
    return this;
  }

  @Override
  public String toString() {
    return String.format("%d:%02d:%02d", h, m, s);
  }
}
