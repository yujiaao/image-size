# image-size
get the image size without load the entire image to memory.the java implemetation of project https://github.com/image-size/image-size

## installation
##### build from source code
```
git clone https://github.com/freeapis/image-size.git
cd image-size
mvn clean install
```
##### add package form repository
```
<dependency>
    <groupId>com.github.yujiaao</groupId>
    <artifactId>image-size</artifactId>
    <version>1.0.6</version>
</dependency>
```
## Usage
##### get the size of arbitrary image
```
Pair<Integer,Integer> size = Image.sizeOf(new File("source.jpg"));
System.out.println(size);
```
###### result output
```$xslt
width:913 height:740
```
##### get the type of arbitrary image
```$xslt
String imageType  = Image.typeOf(new File("source.jpg"));
System.out.println(imageType);
```
##### batch get remote image size 
```$xslt
    List<String> slowBigPic  = new ArrayList<>();

    slowBigPic.add(  "https://www.treehugger.com/thmb/OqlgsKsbrIxySurBnvjiAE-mX2Q=/4000x2666/filters:no_upscale():max_bytes(150000):strip_icc()/baltimore-oriole--icterus-galbula--two-male-birds-fighting-1144653112-7414d71318d34806be3ce04afb3d26f0.jpg");
    slowBigPic.add(  "https://tse3-mm.cn.bing.net/th/id/OIP-C.FOv7BlTdVdis9Ws4p1tcuAHaLH?pid=ImgDet&w=1332&h=1998&rs=1");
    slowBigPic.add(  "https://ts1.cn.mm.bing.net/th/id/R-C.729c655c7b6a29eb428f767c55da93c2?rik=AsyTNrdrw%2faBPg&riu=http%3a%2f%2fwww.olsensgrain.com%2fwp-content%2fuploads%2f2015%2f11%2fDollarphotoclub_72459261-e1448388429924.jpg&ehk=aPrZoyGea3adm0%2fxKO0s5G7gPF%2fEW%2fO%2b%2fLUho%2bqe%2fug%3d&risl=&pid=ImgRaw&r=0");
    
    
    StopWatch sw = new StopWatch();
    sw.start();
    List<Pair<String, Pair<Integer,Integer>>> sizes = Image.batchGet(slowBigPic);
    sw.stop();
    
    sizes.forEach(size  ->
            {
                System.out.println("url:" + size.getLeft() + " width X height:" + size.getRight());
            }
    );
    System.out.println("time:"+sw.getTime()+"ms");

```
###### result output
```$xslt
JPG
```
### Supported Image Format
```$xslt
JPG
PNG
ICO
BMP
GIF
PSD
WEBP
```
