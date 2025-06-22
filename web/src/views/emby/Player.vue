<template>
    <!-- <a-page-header @back="() => router.back()">
        <template #title>
            <div>
                返回
            </div>
        </template>
    </a-page-header> -->
    <video-player v-if="playerId && playerId" :key="playerId" ref="videoPlayerRef" :item-id="playerId"
        style="height: 70vh; max-width: 100%" />
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed, onDeactivated } from 'vue'
import VideoPlayer from "@/components/VideoPlayer.vue";
import request from "@/utils/request";
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';

// 示例播放地址，可替换为任意 URL（支持 HLS、MP4 等）
const videoPlayerRef = ref(null);
const player = computed(() => videoPlayerRef.value?.player());
const playerId = ref(null);

const route = useRoute();
const router = useRouter();
onMounted(() => {
    const { id } = route.params;
    if (!id) {
        message.error("参数错误");
        return;
    }
    playerId.value = Number(id);
    // const { data } = await request.get(`/api/emby-item/detail/${id}`);
    // const poster = getItemImage(data)
    // Object.assign(mediaDetail, { ...data, poster: poster });
})

onBeforeUnmount(() => {
    if (player) {
        player.dispose()
    }
})
onDeactivated(() => {
    player.value = null
})
</script>

<style scoped>
.video-container {
    max-width: 100%;
    margin: 0 auto;
}

.video-js {
    width: 100%;
    height: auto;
}
</style>