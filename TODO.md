- [ ] Inject into on movement & do 8x4x5 (basically what you're looking at) sending once shadow by lighting has been
  implemented.
  (Pulled directly from MixinPlayerEntity)

```java
@Override
public void setPos(double x, double y, double z) {
  double i = prevX, j = prevY, k = prevZ;
  super.setPos(x, y, z);
}
```