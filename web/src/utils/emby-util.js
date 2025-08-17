import { useAppStore } from '@/stores/app'
export const getThumb = (item) => {
    const { config } = useAppStore();
    if (!config.emby_server) {
        console.warn('无法获取图片,emby_server未配置', item);
    }
    if (!item) {
        return null;
    }
    return `${config.emby_server}/emby/Items/${item.Id}/Images/Thumb?maxWidth=700&quality=100`;
}

export const getPrimary = (item) => {
    const { config } = useAppStore();
    if (!config.emby_server) {
        console.warn('无法获取图片,emby_server未配置', item);
    }
    if (!item) {
        return null;
    }
    return `${config.emby_server}/emby/Items/${item.Id}/Images/Primary?maxWidth=700&quality=100`;
}

export const Colorful = (str) => {
    // ant-design-vue 官方内置 Tag 颜色（共 13 个）
    const presetColors = [
        'pink', 'orange', 'cyan', 'green',
        'blue', 'purple', 'geekblue', 'magenta', 'volcano',
        'gold', 'lime'
    ];

    if (!str) return 'default';
    
    if (str === 'SUCCESS') {
        return 'green';
    } else if (str === 'DONE') {
        return '#555';
    } else if (str === 'FAIL') {
        return 'red';
    }

    // 1. 简单哈希
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }

    // 2. 映射到 0~12 的索引
    const index = Math.abs(hash) % presetColors.length;

    // 3. 返回对应颜色
    return presetColors[index];
}

export const mediaStatus = {
    "NONE":"NONE",
    "PENDING":"待处理",
    "SUCCESS":"成功",
    "FAIL":"失败",
    "DONE":"完成",
    "DELETED":"删除",
}

export const GetMediaStatus = (status) => {
    return mediaStatus[status] || status;
}



export const mediaTypes = {
    "ENCODE": "编码",
    "CUT": "剪切",
    "SUBTITLE": "转录",
    "TRANSLATE": "翻译",
    "MOVE": "移动",
}

export const GetMediaInfoType = (type) => {
    return mediaTypes[type] || type;
}