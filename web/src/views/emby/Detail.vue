<template>
    <a-skeleton active v-if="loading" />
    <div v-else class="movie-container">
        <div class="background-overlay"></div>
        <div class="content">
            <!-- PC 端布局 -->
            <div v-if="!isMobile" class="pc-layout">
                <div class="poster-section">
                    <img class="poster" :src="mediaDetail.poster" alt="poster" />
                </div>
                <div class="info-section">
                    <h1 class="title">{{ mediaDetail.SortName }}</h1>
                    <div class="buttons">
                        <a-space>
                            <a-button type="primary" @click="playHandler" :icon="h(PlayCircleFilled)">播放</a-button>
                            <a-button type="primary" @click="openEmbyPage" :icon="h(HeartFilled)">Emby</a-button>
                            <a-button type="primary" :icon="h(DeleteFilled)">删除</a-button>
                        </a-space>
                    </div>
                </div>
            </div>

            <!-- 移动端布局 -->
            <div v-else class="mobile-layout">
                <div class="mobile-header" :style="{ backgroundImage: `url(${mediaDetail.poster})` }">
                    <div class="mobile-overlay">
                        <h1 class="title mobile">Last: Naruto the Movie</h1>
                        <div class="buttons mobile">
                            <a-space>
                                <a-button type="primary" @click="playHandler" :icon="h(PlayCircleFilled)">播放</a-button>
                                <a-button type="primary" @click="openEmbyPage" :icon="h(HeartFilled)">收藏</a-button>
                                <a-button type="primary" :icon="h(DeleteFilled)">删除</a-button>
                            </a-space>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 标签区域 -->
            <div class="tag-section">
                <span class="tag" :style="{ backgroundColor: token.colorPrimaryBgHover }"
                    v-for="tag in mediaDetail.TagItems" :key="tag">{{ tag.Name }}</span>
            </div>

            <!-- 演职人员横向滑动（共用） -->
            <template v-if="mediaDetail.People?.length > 0">
                <h2 class="section-title">演职人员</h2>
                <div class="cast-scroll">
                    <div class="cast-card" v-for="people in mediaDetail.People" :key="people.Id"
                        @click="personFilterHandler(people.Id)">
                        <div class="avatar">
                            {{ people.Name }}
                        </div>
                        <div class="cast-name">{{ people.Name }}</div>
                        <div class="cast-role">{{ people.Type }}</div>
                    </div>
                </div>
            </template>

            <!-- 附加内容 -->
            <template v-if="mediaDetail.specialFeatures && mediaDetail.specialFeatures.length > 0">
                <h2 class="section-title">附加内容</h2>
                <div class="similar-scroll">
                    <div class="similar-card" :style="{}" v-for="item in similarMovies" :key="item.title">
                        <img class="poster-small" :src="posterUrl" alt="similar" />
                        <div class="similar-title">
                            <ellipsis :length="40" :tooltip="true" :line="3">
                                {{ item.title }}
                            </ellipsis>
                        </div>
                    </div>
                </div>
            </template>

            <!-- 相似推荐区域 -->
            <template v-if="similarMovies && similarMovies.length > 0">
                <h2 class="section-title">相似推荐</h2>
                <div class="similar-scroll">
                    <div class="similar-card" v-for="item in similarMovies" :key="item.Id">
                        <img class="poster-small" :src="getItemImage(item)" alt="similar"
                            @click="toSimilarItemHandler(item)" />
                        <div class="similar-title">
                            <ellipsis :length="40" :tooltip="true" :line="3">
                                {{ item.Name }}
                            </ellipsis>
                        </div>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup>
import { useAppStore } from "@/stores/app";
import { ref, onActivated, computed, reactive, h } from "vue";
import { useRoute, useRouter } from 'vue-router';
import request from "@/utils/request";
import { getItemImage } from "@/utils/emby-util";
import { message, theme } from "ant-design-vue";
import { PlayCircleFilled, DeleteFilled, HeartFilled } from '@ant-design/icons-vue';
import Ellipsis from "@/components/Ellipsis.vue";
const { useToken } = theme;
const { token } = useToken();

const { isMobile, config } = useAppStore();

const router = useRouter();
const route = useRoute();

onActivated(() => {
    // 从路由参数中获取parentId
    const { id } = route.params;
    if (!id) {
        message.error("parentId不能为空");
        return;
    }
    fetchData(id)
})

const loading = ref(false);

const mediaDetail = reactive({});
const fetchData = async (id) => {
    loading.value = true;
    try {
        const { data } = await request.get(`/api/emby-item/detail/${id}`);
        const poster = getItemImage(data)
        Object.assign(mediaDetail, { ...data, poster: poster });
        fetchSimilar();
    } finally {
        loading.value = false;
    }
}

const similarMovies = ref([]);
const similarLoading = ref(false);
const fetchSimilar = async () => {
    similarLoading.value = true;
    const { data } = await request.get(`/api/emby-item/similar/${mediaDetail.Id}`);
    similarMovies.value = data;
    similarLoading.value = false;
}

const playHandler = async () => {
    router.push({ name: 'EmbyPlayer', params: { id: mediaDetail.Id } })
};

const personFilterHandler = async (personId) => {
    console.log('personId', personId);
    router.push({ name: 'EmbyList', query: { personIds: personId } })
};

// 在Emby中打开
const openEmbyPage = () => {
    const emby_server = config.emby_server;
    if (!emby_server) {
        message.error("未获取到emby_server");
        return;
    }
    const openUrl = `${emby_server}/web/index.html#!/item?id=${mediaDetail.Id}&serverId=${mediaDetail.ServerId}`;
    console.log('openUrl', openUrl);
    window.open(
        openUrl,
        "_blank"
    );
};

const toSimilarItemHandler = (item) => {
    fetchData(item.Id)
    // 滚动到页面顶部
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

const cardStyle = computed(() => {
    return {
        borderRadius: token.borderRadius + 'px',
        border: '1px solid ' + token.colorBorder
    };
})
</script>

<style scoped lang="less">
@button-bg: #ffffff22;
@button-bg-hover: #ffffff44;
@card-bg: #ffffff11;
@border-radius: 8px;

.movie-container {
    min-height: 100vh;
    position: relative;

    .background-overlay {
        position: absolute;
        inset: 0;
        z-index: 1;
    }

    .content {
        position: relative;
        z-index: 2;
        padding: 20px;

        // PC 布局
        .pc-layout {
            display: flex;
            flex-direction: row;
            gap: 20px;
            flex-wrap: wrap;
            margin-bottom: 30px;

            .poster-section {
                flex-shrink: 0;

                .poster {
                    height: 366px;
                    object-fit: cover;
                    border-radius: @border-radius;
                }
            }

            .info-section {
                flex: 1;
                display: flex;
                flex-direction: column;
                justify-content: center;

                .title {
                    font-size: 28px;
                    margin-bottom: 20px;
                }
            }
        }

        // 移动端布局
        .mobile-layout {
            margin-bottom: 30px;

            .mobile-header {
                width: 100%;
                height: 300px;
                background-size: cover;
                background-position: center;
                position: relative;
                border-radius: @border-radius;
                overflow: hidden;

                .mobile-overlay {
                    background: linear-gradient(to top, rgba(0, 0, 0, 0.8), rgba(0, 0, 0, 0.2));
                    position: absolute;
                    inset: 0;
                    padding: 20px;
                    display: flex;
                    flex-direction: column;
                    justify-content: flex-end;
                    align-items: center;
                    text-align: center;

                    .title.mobile {
                        font-size: 22px;
                        margin-bottom: 12px;
                    }
                }
            }
        }

        // 标签区域
        .tag-section {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 20px;

            .tag {
                padding: 6px 12px;
                border-radius: 20px;
                font-size: 14px;
                cursor: pointer;
            }
        }

        // 通用标题
        .section-title {
            font-size: 20px;
            margin: 20px 0 10px;
        }

        // 横滑：演职人员
        .cast-scroll {
            display: flex;
            gap: 16px;
            overflow-x: auto;
            padding-bottom: 10px;
            scrollbar-width: thin;

            .cast-card {
                flex: 0 0 auto;
                cursor: pointer;
                background: @card-bg;
                padding: 10px;
                width: 140px;
                border-radius: @border-radius;
                text-align: center;

                .avatar {
                    width: 100%;
                    height: 160px;
                    object-fit: cover;
                    border-radius: 6px;
                    margin-bottom: 8px;
                }

                .cast-name {
                    font-weight: bold;
                }

                .cast-role {
                    font-size: 12px;
                    color: #ccc;
                }
            }
        }

        // 横滑：相似推荐
        .similar-scroll {
            display: flex;
            gap: 16px;
            overflow-x: auto;
            padding-bottom: 10px;
            scrollbar-width: thin;

            .similar-card {
                flex: 0 0 auto;
                width: 120px;
                text-align: center;

                :hover {
                    transform: scale(1.05);
                    transition: transform 0.3s ease;
                }

                .poster-small {
                    width: 100%;
                    height: 180px;
                    object-fit: cover;
                    border-radius: 6px;
                    margin-bottom: 6px;
                }
            }
        }
    }
}
</style>