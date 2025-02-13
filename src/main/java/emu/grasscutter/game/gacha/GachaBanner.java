package emu.grasscutter.game.gacha;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.net.proto.GachaInfoOuterClass.GachaInfo;
import emu.grasscutter.net.proto.GachaUpInfoOuterClass.GachaUpInfo;

public class GachaBanner {
	private int gachaType;
	private int scheduleId;
	private String prefabPath;
	private String previewPrefabPath;
	private String titlePath;
	private int costItem;
	private int beginTime;
	private int endTime;
	private int sortId;
	private int[] rateUpItems1;
	private int[] rateUpItems2;
	private int minItemType = 1;
	private int maxItemType = 2;
	private int eventChance = 50; // Chance to win a featured event item
	private int softPity = 75;
	private int hardPity = 90;
	private BannerType bannerType = BannerType.STANDARD;
	
	public int getGachaType() {
		return gachaType;
	}

	public BannerType getBannerType() {
		return bannerType;
	}

	public int getScheduleId() {
		return scheduleId;
	}

	public String getPrefabPath() {
		return prefabPath;
	}

	public String getPreviewPrefabPath() {
		return previewPrefabPath;
	}

	public String getTitlePath() {
		return titlePath;
	}

	public int getCostItem() {
		return costItem;
	}

	public int getBeginTime() {
		return beginTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public int getSortId() {
		return sortId;
	}

	public int[] getRateUpItems1() {
		return rateUpItems1;
	}

	public int[] getRateUpItems2() {
		return rateUpItems2;
	}

	public int getMinItemType() {
		return minItemType;
	}
	
	public int getMaxItemType() {
		return maxItemType;
	}
	
	public int getSoftPity() {
		return softPity - 1;
	}

	public int getHardPity() {
		return hardPity - 1;
	}

	public int getEventChance() {
		return eventChance;
	}

	public GachaInfo toProto() {
		String record = "http://" + (Grasscutter.getConfig().DispatchServerPublicIp.isEmpty() ? Grasscutter.getConfig().DispatchServerIp : Grasscutter.getConfig().DispatchServerPublicIp) + "/gacha";
		
		GachaInfo.Builder info = GachaInfo.newBuilder()
				.setGachaType(this.getGachaType())
				.setScheduleId(this.getScheduleId())
				.setBeginTime(this.getBeginTime())
				.setEndTime(this.getEndTime())
				.setCostItemId(this.getCostItem())
	            .setCostItemNum(1)
	            .setGachaPrefabPath(this.getPrefabPath())
	            .setGachaPreviewPrefabPath(this.getPreviewPrefabPath())
	            .setGachaProbUrl(record)
	            .setGachaProbUrlOversea(record)
	            .setGachaRecordUrl(record)
	            .setGachaRecordUrlOversea(record)
	            .setTenCostItemId(this.getCostItem())
	            .setTenCostItemNum(10)
	            .setLeftGachaTimes(Integer.MAX_VALUE)
	            .setGachaTimesLimit(Integer.MAX_VALUE)
	            .setGachaSortId(this.getSortId());
		
		if (this.getTitlePath() != null) {
			info.setGachaTitlePath(this.getTitlePath());
		}
		
		if (this.getRateUpItems1().length > 0) {
			GachaUpInfo.Builder upInfo = GachaUpInfo.newBuilder().setItemParentType(1);
			
			for (int id : getRateUpItems1()) {
				upInfo.addItemIdList(id);
				info.addMainNameId(id);
			}
			
			info.addGachaUpInfoList(upInfo);
		}
		
		if (this.getRateUpItems2().length > 0) {
			GachaUpInfo.Builder upInfo = GachaUpInfo.newBuilder().setItemParentType(2);
			
			for (int id : getRateUpItems2()) {
				upInfo.addItemIdList(id);
				if (info.getSubNameIdCount() == 0) {
					info.addSubNameId(id);
				}
			}
			
			info.addGachaUpInfoList(upInfo);
		}
		
		return info.build();
	}
	
	public enum BannerType {
		STANDARD, EVENT, WEAPON;
	}
}
