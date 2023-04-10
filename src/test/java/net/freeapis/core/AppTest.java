package net.freeapis.core;

import net.freeapis.core.utils.HttpClientUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for simple App.
 */
//@Disabled
public class AppTest
{

    @BeforeAll
    public static void setUp() {
        HttpClientUtils.builder();
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() throws Exception
    {
        Pair<Integer,Integer> size = Image
                .sizeOf(new File(ClassLoader.getSystemResource("source.jpg").getFile()));
        System.out.println("width:" + size.getLeft() + " height:" + size.getRight());

        // System.out.println(Image.sizeOf(new File("D:\\GraphicsMagic\\doc-convert\\bug.jpg")));
        System.out.println(Image.sizeOf(new File(ClassLoader.getSystemResource("bug.jpg").getFile())));
    }


    /**
     * nginx config:
     *   location /limit {
     *       root /var/www;
     *       limit_rate_after 50k;
     *       limit_rate       5k;
     *   }
     */
    @Test
    public void shouldAnswerWithSlowNet() {
            String slowBigPic  = "http://10.168.1.193/limit/3.jpg";

        StopWatch sw = new StopWatch();
        sw.start();
        Pair<Integer,Integer> size = Image.doGet(slowBigPic);
        sw.stop();
        System.out.println("width:" + size.getLeft() + " height:" + size.getRight());
        System.out.println("time:"+sw.getTime());

    }


    @Test
    public void batchWithSlowNet() {
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

    }




    @Test
    public void batchTest() {
        List<String> slowBigPic  = new ArrayList<>();

        slowBigPic.add(  "http://10.168.1.193/limit/1.jpg");
        slowBigPic.add(  "http://10.168.1.193/limit/2.jpg");
        slowBigPic.add(  "http://10.168.1.193/limit/3.jpg");


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

    }

    @Test public void testWebp(){
        Pair<Integer, Integer> size = Image.sizeOf(this.getClass().getResourceAsStream("/sample.webp"),
                50_408);
        assertEquals(1050,size.getLeft());
        assertEquals(700,size.getRight());
    }

    @Test public void testBadCase1Jpeg(){
        Pair<Integer, Integer> size = Image.sizeOf(this.getClass().getResourceAsStream("/55.jpg"),
                4_650_249);

        assertEquals(6000,size.getLeft());
        assertEquals(2967,size.getRight());
    }

}
