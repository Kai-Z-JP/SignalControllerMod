package jp.masa.signalcontrollermod.block.tileentity;

import jp.masa.signalcontrollermod.gui.signalcontroller.SignalType;
import jp.masa.signalcontrollermod.utils.BlockPos;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.electric.TileEntitySignal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TileEntitySignalController extends TileEntityCustom {
    private SignalType signalType;
    private List<BlockPos> nextSignal;
    private List<BlockPos> displayPos;
    private boolean above;
    private boolean last;
    private boolean repeat;
    private boolean reducedSpeed;

    public TileEntitySignalController() {
        this.signalType = SignalType.signal3;
        this.nextSignal = new ArrayList<BlockPos>(Arrays.asList(new BlockPos(0, 0, 0)));
        this.displayPos = new ArrayList<BlockPos>(Arrays.asList(new BlockPos(0, 0, 0)));
        this.above = false;
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

            for (BlockPos pos : this.nextSignal) {
                Object nextSignal = this.getSignal(world, pos.X, pos.Y, pos.Z);

                if (nextSignal instanceof Integer) {
                    nextSignalList.add((int) nextSignal);
                }
            }
            int nextSignalLevel = (this.last) ? 1 : nextSignalList.stream().mapToInt(v -> v).max().orElse(0);

            // RS入力(停止現示)
            boolean isRSPowered = world.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord); //レッドストーン確認

            //表示する信号機の制御
            //変化したときだけ変更するようにすることで負荷を減らすこと
            int signalLevel = (this.repeat && (3 <= nextSignalLevel && nextSignalLevel <= 4)) ? nextSignalLevel : this.signalType.upSignalLevel(nextSignalLevel);
            Object currentSignal;
            if (signalLevel > MAXSIGNALLEVEL) signalLevel = MAXSIGNALLEVEL;
            if (isRSPowered) signalLevel = 1;

            if (this.above) {
                int aboveY = this.searchSignalAboveY(world);
                if (1 <= aboveY) {
                    currentSignal = getSignal(world, this.xCoord, aboveY, this.zCoord);
                    if (currentSignal != null && (int) currentSignal != signalLevel)
                        setSignal(world, this.xCoord, aboveY, this.zCoord, signalLevel);
                }
            }

            for (BlockPos pos : this.displayPos) {
                if (!(pos.X == 0 && pos.Y == 0 && pos.Z == 0)) {
                    currentSignal = getSignal(world, pos.X, pos.Y, pos.Z);
                    if (currentSignal != null && (int) currentSignal != signalLevel) {
                        setSignal(world, pos.X, pos.Y, pos.Z, signalLevel);
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
        if (nbt.hasKey("nextSignalList") && nbt.hasKey("displayPosList")) {
            // nextSignal
            NBTTagList nextSignalList = nbt.getTagList("nextSignalList", 10);
            for (int i = 0; i < nextSignalList.tagCount(); i++) {
                NBTTagCompound tag = nextSignalList.getCompoundTagAt(i);
                this.nextSignal.add(BlockPos.readFromNBT(tag));
            }
            // displayPos
            NBTTagList displayPosList = nbt.getTagList("displayPosList", 10);
            for (int i = 0; i < displayPosList.tagCount(); i++) {
                NBTTagCompound tag = displayPosList.getCompoundTagAt(i);
                this.displayPos.add(BlockPos.readFromNBT(tag));
            }
        } else {
            int nextSignalSize = nbt.getInteger("nextSignalSize");
            if (nextSignalSize == 0) {
                this.nextSignal.add(BlockPos.fromIntArray(this.getIntArray(nbt, "nextSignal0")));
            } else {
                for (int i = 0; i < nextSignalSize; i++) {
                    this.nextSignal.add(BlockPos.fromIntArray(this.getIntArray(nbt, "nextSignal" + i)));
                }
            }
            // displayPos
            int displayPosSize = nbt.getInteger("displayPosSize");
            if (displayPosSize == 0) {
                this.displayPos.add(BlockPos.fromIntArray(this.getIntArray(nbt, "displayPos")));
            } else {
                for (int i = 0; i < displayPosSize; i++) {
                    this.displayPos.add(BlockPos.fromIntArray(this.getIntArray(nbt, "displayPos" + i)));

                }
            }
        }
        this.above = nbt.getBoolean("above");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("signalType", this.signalType.toString());
        nbt.setBoolean("last", this.last);
        nbt.setBoolean("repeat", this.repeat);
        nbt.setBoolean("reducedSpeed", this.reducedSpeed);
        // nextSignal
        NBTTagList nextSignalList = new NBTTagList();
        this.nextSignal.forEach(blockPos -> nextSignalList.appendTag(BlockPos.writeToNBT(blockPos)));
        nbt.setTag("nextSignalList", nextSignalList);

        // displayPos
        NBTTagList displayPosList = new NBTTagList();
        this.displayPos.forEach(blockPos -> displayPosList.appendTag(BlockPos.writeToNBT(blockPos)));
        nbt.setTag("displayPosList", displayPosList);

        nbt.setBoolean("above", this.above);
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

    public List<BlockPos> getNextSignal() {
        return nextSignal;
    }

    public void setNextSignal(List<BlockPos> nextSignal) {
        this.nextSignal = nextSignal;
    }

    public boolean addNextSignal(BlockPos nextSignalPos) {
        BlockPos pos000 = new BlockPos(0, 0, 0);
        for (BlockPos pos : this.nextSignal) {
            if (pos.equals(nextSignalPos)) {
                return false;
            } else if (pos.equals(new BlockPos(0, 0, 0))) {
                pos000 = pos;
            }
        }
        this.nextSignal.remove(pos000);
        this.nextSignal.add(nextSignalPos);
        return true;
    }

    public List<BlockPos> getDisplayPos() {
        return displayPos;
    }

    public void setDisplayPos(List<BlockPos> displayPos) {
        this.displayPos = displayPos;
    }

    public boolean addDisplayPos(BlockPos displayPos) {
        BlockPos pos000 = new BlockPos(0, 0, 0);
        for (BlockPos pos : this.displayPos) {
            if (pos.equals(displayPos)) {
                return false;
            } else if (pos.equals(new BlockPos(0, 0, 0))) {
                pos000 = pos;
            }
        }
        this.displayPos.remove(pos000);
        this.displayPos.add(displayPos);
        return true;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isReducedSpeed() {
        return reducedSpeed;
    }

    public void setReducedSpeed(boolean reducedSpeed) {
        this.reducedSpeed = reducedSpeed;
    }

    public boolean isAbove() {
        return above;
    }

    public void setAbove(boolean above) {
        this.above = above;
    }
}
