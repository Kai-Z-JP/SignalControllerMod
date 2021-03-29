package jp.masa.signalcontrollermod.block.tileentity;

import jp.masa.signalcontrollermod.gui.signalcontroller.SignalType;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.electric.TileEntitySignal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TileEntitySignalController extends TileEntityCustom {
    private SignalType signalType;
    private int[][] nextSignal;
    private int[][] displayPos;
    private int signalLevel;
//    private boolean above;
    private boolean last;
    private boolean repeat;
    private  boolean reducedSpeed;

    public TileEntitySignalController() {
        this.signalType = SignalType.signal3;
        this.nextSignal = new int[][]{{0, 0, 0}};
        this.displayPos = new int[][]{{0, 0, 0}};
        this.signalLevel = 1;
//        this.above = false;
        this.last = false;
        this.repeat = false;
        this.reducedSpeed = false;
    }

    @Override
    public void updateEntity() {
        World world = this.getWorldObj();
        if (!world.isRemote) {
            int MAXSIGNALLEVEL = 6;
            List<Integer> nextSignalList = new ArrayList<>();

            for (int[] pos : this.nextSignal) {
//                Object nextSignal = this.getSignal(world, pos[0], pos[1], pos[2]);
                Object nextSignal = this.getSignalFromController(world, pos[0], pos[1], pos[2]);

                if (nextSignal instanceof Integer) {
                    nextSignalList.add((int) nextSignal);
                }
            }
            int nextSignalLevel = (this.last) ? 1 : nextSignalList.stream().mapToInt(v -> v).max().orElse(0);

            // RS入力(停止現示)
            boolean isRSPowered = world.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord); //レッドストーン確認

            //表示する信号機の制御
            //変化したときだけ変更するようにすることで負荷を減らすこと
            this.signalLevel = (this.repeat && (3 <= nextSignalLevel && nextSignalLevel <= 4)) ? nextSignalLevel : this.signalType.upSignalLevel(nextSignalLevel);
            Object currentSignal;
            if (this.signalLevel > MAXSIGNALLEVEL) this.signalLevel = MAXSIGNALLEVEL;
            if (isRSPowered) this.signalLevel = 1;

//            if (this.above) {
//                int aboveY = this.searchSignalAboveY(world);
//                if(1 <= aboveY) {
//                    currentSignal = getSignal(world, this.xCoord, aboveY, this.zCoord);
//                    if (currentSignal != null && (int) currentSignal != signalLevel) setSignal(world, this.xCoord, aboveY, this.zCoord, signalLevel);
//                }
//            }

            for (int[] pos : this.displayPos) {
                if (!(pos[0] == 0 && pos[1] == 0 && pos[2] == 0)) {
                    currentSignal = getSignal(world, pos[0], pos[1], pos[2]);
                    if (currentSignal != null && (int) currentSignal != this.signalLevel) {
                        setSignal(world, pos[0], pos[1], pos[2], this.signalLevel);
                    }
                }
            }
        }
    }

    private Object getSignal(World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntitySignal)
            return NGTUtil.getField(TileEntitySignal.class, tileEntity, "signalLevel");
        return null;
    }

    private void setSignal(World world, int x, int y, int z, int level) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntitySignal) {
            ((TileEntitySignal) tileEntity).setElectricity(x, y, z, level);
        }
    }

    private Object getSignalFromController(World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntitySignalController)
            return ((TileEntitySignalController) tileEntity).getSignalLevel();
        return null;
    }

    private int searchSignalAboveY(World world) {
        int searchMaxCount = 32;

        for (int i = 1; i <= searchMaxCount; i++) {
            int y = this.yCoord + i;
            TileEntity tileEntity = world.getTileEntity(this.xCoord, y, this.zCoord);
            if (tileEntity instanceof TileEntitySignal) return y;
        }
        return 0;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.signalType = SignalType.getType(nbt.getString("signalType"));
        this.last = nbt.getBoolean("last");
        this.repeat = nbt.getBoolean("repeat");
        this.reducedSpeed = nbt.getBoolean("reducedSpeed");
        // nextSignal
        int nextSignalSize = nbt.getInteger("nextSignalSize");
        if (nextSignalSize == 0) {
            this.nextSignal[0] = this.getIntArray(nbt,"nextSignal0");
        } else {
            this.nextSignal = new int[nextSignalSize][3];
            for (int i = 0; i < nextSignalSize; i++) {
                this.nextSignal[i] = this.getIntArray(nbt, "nextSignal" + i);
            }
        }
        // displayPos
        int displayPosSize = nbt.getInteger("displayPosSize");
        if (displayPosSize == 0) {
            this.displayPos[0] = this.getIntArray(nbt, "displayPos");
        } else {
            this.displayPos = new int[displayPosSize][3];
            for (int i = 0; i < displayPosSize; i++) {
                this.displayPos[i] = this.getIntArray(nbt, "displayPos" + i);

            }
        }
//        this.above = nbt.getBoolean("above");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("signalType", this.signalType.toString());
        nbt.setBoolean("last", this.last);
        nbt.setBoolean("repeat", this.repeat);
        nbt.setBoolean("reducedSpeed", this.reducedSpeed);
        // nextSignal
        int nextSignalSize = this.nextSignal.length;
        nbt.setInteger("nextSignalSize", nextSignalSize);
        for (int i = 0; i < nextSignalSize; i++) {
            nbt.setIntArray("nextSignal" + i, this.nextSignal[i]);
        }
        // displayPos
        int displayPosSize = this.displayPos.length;
        nbt.setInteger("displayPosSize", displayPosSize);
        for (int i = 0; i < displayPosSize; i++) {
            nbt.setIntArray("displayPos" + i, this.displayPos[i]);
        }
//        nbt.setBoolean("above", this.above);
    }

    private int[] getIntArray(NBTTagCompound nbt, String key) {
        int[] intArray = nbt.getIntArray(key);
        return intArray.length == 0 ? new int[3] : intArray;
    }

    public SignalType getSignalType() {
        return (this.signalType == null) ? this.signalType = SignalType.signal3 : this.signalType;
    }

    public void setSignalType(SignalType signalType) {
        this.signalType = signalType;
    }

    public int[][] getNextSignal() {
        return nextSignal;
    }

    public void setNextSignal(int[][] nextSignal) {
        this.nextSignal = nextSignal;
    }

    public boolean addNextSignal(int[] nextSignalPos) {
        List<int[]> nextSignalList = new ArrayList<>(Arrays.asList(this.nextSignal));
        int[] pos000 = new int[]{0, 0, 0};
        for (int[] pos : nextSignalList) {
            if (Arrays.equals(pos, nextSignalPos)) {
                return false;
            } else if (Arrays.equals(pos, new int[]{0, 0, 0})) {
                pos000 = pos;
            }
        }
        nextSignalList.remove(pos000);
        nextSignalList.add(nextSignalPos);
        this.nextSignal = nextSignalList.toArray(new int[nextSignalList.size()][]);
        return true;
    }

    public int[][] getDisplayPos() {
        return displayPos;
    }

    public void setDisplayPos(int[][] displayPos) {
        this.displayPos = displayPos;
    }

    public boolean addDisplayPos(int[] displayPos) {
        List<int[]> displayPosList = new ArrayList<>(Arrays.asList(this.displayPos));
        int[] pos000 = new int[]{0, 0, 0};
        for (int[] pos : displayPosList) {
            if (Arrays.equals(pos, displayPos)) {
                return false;
            } else if (Arrays.equals(pos, new int[]{0, 0, 0})) {
                pos000 = pos;
            }
        }
        displayPosList.remove(pos000);
        displayPosList.add(displayPos);
        this.displayPos = displayPosList.toArray(new int[displayPosList.size()][]);
        return true;
    }

    public int getSignalLevel() { return this.signalLevel; }

    public void setSignalLevel(int signalLevel) { this.signalLevel = signalLevel; }

    public boolean isLast() { return last; }

    public void setLast(boolean last) { this.last = last; }

    public boolean isRepeat() { return repeat; }

    public void setRepeat(boolean repeat) { this.repeat = repeat; }

    public boolean isReducedSpeed() { return reducedSpeed; }

    public void setReducedSpeed(boolean reducedSpeed) { this.reducedSpeed = reducedSpeed; }

//    public boolean isAbove() { return above; }

//    public void setAbove(boolean above) { this.above = above; }
}
