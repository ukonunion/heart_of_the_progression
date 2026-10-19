package dev.rpgprogression.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SkintActivationModelTest {
    private SkintActivationModel model()throws Exception{
        try(var input=getClass().getResourceAsStream("/assets/rpgprogression/models/block/travelskint-animation.json")){
            assertNotNull(input);return new SkintActivationModel(JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject());
        }
    }
    @Test void authoredEndpointsAndDurationArePreserved()throws Exception{
        var m=model();assertEquals(1.25,m.length);var big=m.bones.getFirst().children().getFirst();
        assertEquals(-22,SkintActivationModel.sample(big,"position",0)[1]);
        assertEquals(-14.27,SkintActivationModel.sample(big,"position",.25)[1],.0001);
        assertEquals(-5.4826,SkintActivationModel.sample(big,"rotation",0)[0],.0001);
        assertArrayEquals(new float[]{0,0,0},SkintActivationModel.sample(big,"position",1.25));
        assertArrayEquals(new float[]{0,0,0},SkintActivationModel.sample(big,"rotation",20));
    }
    @Test void secondCrystalKeepsItsOwnMotionAndBindPose()throws Exception{
        var small=model().bones.getFirst().children().get(1);
        assertEquals(-2/16f,small.origin()[1]);assertEquals(-17,SkintActivationModel.sample(small,"position",0)[1]);
        assertEquals(-11.02,SkintActivationModel.sample(small,"position",.83333)[1],.0001);
        assertArrayEquals(new float[]{1,1,1},SkintActivationModel.sample(small,"scale",.5));
    }
    @Test void partialFramesAreContinuousAndRiseIntoTheActivePose()throws Exception{
        var big=model().bones.getFirst().children().getFirst();
        float before=SkintActivationModel.sample(big,"position",1)[1],after=SkintActivationModel.sample(big,"position",1.05)[1];
        assertTrue(before<after&&after<0);
        for(double key:new double[]{.25,.83333,1.25})assertEquals(SkintActivationModel.sample(big,"position",key-1e-6)[1],SkintActivationModel.sample(big,"position",key+1e-6)[1],.001);
        for(int i=0;i<=125;i++)for(var child:model().bones.getFirst().children())for(float value:SkintActivationModel.sample(child,"position",i/100.0))assertTrue(Float.isFinite(value));
    }
    @Test void allVisibleFacesHaveUsableTextureCoordinatesAndNormals()throws Exception{
        var root=model().bones.getFirst();var bones=new java.util.ArrayList<SkintActivationModel.Bone>();bones.add(root);bones.addAll(root.children());
        assertEquals(44,bones.stream().mapToInt(b->b.faces().size()).sum());
        for(var b:bones)for(var face:b.faces()){
            assertTrue(face.texture()>=0&&face.texture()<12);float[] n=face.normal();assertEquals(1,n[0]*n[0]+n[1]*n[1]+n[2]*n[2],.0001);
            for(var vertex:face.vertices()){assertEquals(5,vertex.length);for(float v:vertex)assertTrue(Float.isFinite(v));assertTrue(vertex[3]>=0&&vertex[3]<=1&&vertex[4]>=0&&vertex[4]<=1);}
        }
    }
}
