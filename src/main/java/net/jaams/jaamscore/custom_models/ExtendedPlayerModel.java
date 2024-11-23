
package net.jaams.jaamscore.custom_models;

import net.minecraftforge.client.IArmPoseTransformer;

import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Mob;
import net.minecraft.client.model.geom.ModelPart;

import net.jaams.jaamscore.entity.CorePlayerEntity;

public class ExtendedPlayerModel<T extends Mob> extends CustomPlayerModel<T> {
	private static final IArmPoseTransformer NO_TRANSFORM = (modelPart, livingEntity, partialTicks) -> {
	};
	private final DualModelPart hatHead;
	private final DualModelPart leftArmSleeve;
	private final DualModelPart rightArmSleeve;
	private final DualModelPart leftLegPants;
	private final DualModelPart rightLegPants;
	private final DualModelPart bodyJacket;

	public ExtendedPlayerModel(ModelPart root, boolean slim) {
		super(root, slim);
		this.hatHead = new DualModelPart(this.head, this.hat);
		this.leftArmSleeve = new DualModelPart(this.leftArm, this.leftSleeve);
		this.rightArmSleeve = new DualModelPart(this.rightArm, this.rightSleeve);
		this.leftLegPants = new DualModelPart(this.leftLeg, this.leftPants);
		this.rightLegPants = new DualModelPart(this.rightLeg, this.rightPants);
		this.bodyJacket = new DualModelPart(this.body, this.jacket);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		this.copyMainParts();
		if (entity instanceof CorePlayerEntity coreEntity) {
			if (this.isHoldingDiamond(entity)) {
				this.adjustArmsForDiamondHold(entity);
			}
			if (coreEntity.isSitting() && !coreEntity.isPassenger()) {
				this.adjustForSittingPose();
			}
			if (coreEntity.isWaving()) {
				animateWave(ageInTicks * 0.1F);
			} else if (coreEntity.isRunning()) {
				animateRun(ageInTicks * 0.15F);
			} else if (coreEntity.isLookingAround()) {
				animateLookAround(ageInTicks * 0.05F);
			}
		}
	}

	private void copyMainParts() {
		this.leftPants.copyFrom(this.leftLeg);
		this.rightPants.copyFrom(this.rightLeg);
		this.leftSleeve.copyFrom(this.leftArm);
		this.rightSleeve.copyFrom(this.rightArm);
		this.jacket.copyFrom(this.body);
		this.hat.copyFrom(this.head);
	}

	private boolean isHoldingDiamond(T entity) {
		return entity.getMainHandItem().is(Items.DIAMOND) || entity.getOffhandItem().is(Items.DIAMOND);
	}

	private void adjustArmsForDiamondHold(T entity) {
		if (entity.getMainHandItem().is(Items.DIAMOND)) {
			this.rightArmSleeve.setRotation(-0.9F, -0.5F, 0.0F);
		}
		if (entity.getOffhandItem().is(Items.DIAMOND)) {
			this.leftArmSleeve.setRotation(-0.9F, 0.5F, 0.0F);
		}
		this.hatHead.setRotation(0.5F, 0.0F, 0.0F);
	}

	private void adjustForSittingPose() {
		this.rightArmSleeve.setRotation(-(float) Math.PI / 5F, 0, 0);
		this.leftArmSleeve.setRotation(-(float) Math.PI / 5F, 0, 0);
		this.rightLegPants.setRotation(-1.4137167F, (float) Math.PI / 10F, 0.07853982F);
		this.leftLegPants.setRotation(-1.4137167F, -(float) Math.PI / 10F, -0.07853982F);
	}

	private void animateWave(float progress) {
		float angle = (float) Math.sin(progress * Math.PI * 2) * 0.5F;
		this.rightArmSleeve.setRotation(-1.0F + angle, 0.0F, 0.0F);
	}

	private void animateRun(float progress) {
		float armSwing = (float) Math.sin(progress * Math.PI * 2) * 1.2F;
		float legSwing = (float) Math.cos(progress * Math.PI * 2) * 1.2F;
		this.rightArmSleeve.setRotation(-0.5F + armSwing, 0.0F, 0.0F);
		this.leftArmSleeve.setRotation(-0.5F - armSwing, 0.0F, 0.0F);
		this.rightLegPants.setRotation(-0.5F + legSwing, 0.0F, 0.0F);
		this.leftLegPants.setRotation(-0.5F - legSwing, 0.0F, 0.0F);
	}

	private void animateLookAround(float progress) {
		float headYaw = (float) Math.sin(progress * Math.PI * 2) * 0.5F;
		float headPitch = (float) Math.cos(progress * Math.PI * 2) * 0.2F;
		this.hatHead.setRotation(headPitch, headYaw, 0.0F);
	}

	private class DualModelPart {
		ModelPart primary;
		ModelPart secondary;

		public DualModelPart(ModelPart primary, ModelPart secondary) {
			this.primary = primary;
			this.secondary = secondary;
		}

		public void setXRot(float xRot) {
			this.primary.xRot = xRot;
			this.secondary.xRot = xRot;
		}

		public void setYRot(float yRot) {
			this.primary.yRot = yRot;
			this.secondary.yRot = yRot;
		}

		public void setZRot(float zRot) {
			this.primary.zRot = zRot;
			this.secondary.zRot = zRot;
		}

		public void setRotation(float xRot, float yRot, float zRot) {
			setXRot(xRot);
			setYRot(yRot);
			setZRot(zRot);
		}
	}
}
