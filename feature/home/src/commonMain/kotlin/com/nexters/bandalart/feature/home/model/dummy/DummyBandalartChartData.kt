/*
 * Copyright 2025 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.feature.home.model.dummy

import com.nexters.bandalart.core.domain.entity.BandalartCellEntity

val dummyBandalartChartData =
    BandalartCellEntity(
        id = 0L,
        title = "넥스터즈 1등하기",
        description = null,
        isCompleted = false,
        parentId = null,
        children =
            listOf(
                BandalartCellEntity(
                    id = 1L,
                    title = "iOS 출시",
                    description = null,
                    isCompleted = false,
                    parentId = 0L,
                    children =
                        listOf(
                            BandalartCellEntity(
                                id = 5L,
                                title = "개발자 계정 만들기",
                                description = null,
                                isCompleted = true,
                                parentId = 1L,
                            ),
                            BandalartCellEntity(
                                id = 6L,
                                title = "MVP 개발",
                                description = null,
                                isCompleted = false,
                                parentId = 1L,
                            ),
                            BandalartCellEntity(
                                id = 7L,
                                title = null,
                                description = null,
                                isCompleted = false,
                                parentId = 1L,
                            ),
                            BandalartCellEntity(
                                id = 8L,
                                title = "코딩 컨벤션 정하기",
                                description = null,
                                isCompleted = true,
                                parentId = 1L,
                            ),
                            BandalartCellEntity(
                                id = 9L,
                                title = "1일 1커밋",
                                description = null,
                                isCompleted = true,
                                parentId = 1L,
                            ),
                        ),
                ),
                BandalartCellEntity(
                    id = 2L,
                    title = "Android 출시",
                    description = null,
                    isCompleted = true,
                    parentId = 0L,
                    children =
                        listOf(
                            BandalartCellEntity(
                                id = 10L,
                                title = "매일 밤새기",
                                description = null,
                                isCompleted = true,
                                parentId = 2L,
                            ),
                            BandalartCellEntity(
                                id = 11L,
                                title = "개발자 계정 만들기",
                                description = null,
                                isCompleted = true,
                                parentId = 2L,
                            ),
                            BandalartCellEntity(
                                id = 12L,
                                title = "MVP 개발",
                                description = null,
                                isCompleted = true,
                                parentId = 2L,
                            ),
                            BandalartCellEntity(
                                id = 13L,
                                title = "코딩 컨벤션 정하기",
                                description = null,
                                isCompleted = true,
                                parentId = 2L,
                            ),
                            BandalartCellEntity(
                                id = 14L,
                                title = "하나라도 더 만들기",
                                description = null,
                                isCompleted = true,
                                parentId = 2L,
                            ),
                        ),
                ),
                BandalartCellEntity(
                    id = 3L,
                    title = "PM 말듣기",
                    description = null,
                    isCompleted = false,
                    parentId = 0L,
                    children =
                        listOf(
                            BandalartCellEntity(
                                id = 15L,
                                title = "화식",
                                description = null,
                                isCompleted = true,
                                parentId = 3L,
                            ),
                            BandalartCellEntity(
                                id = 16L,
                                title = "클라이밍",
                                description = null,
                                isCompleted = false,
                                parentId = 3L,
                            ),
                            BandalartCellEntity(
                                id = 17L,
                                title = "카페",
                                description = null,
                                isCompleted = true,
                                parentId = 3L,
                            ),
                            BandalartCellEntity(
                                id = 18L,
                                title = "산책",
                                description = null,
                                isCompleted = false,
                                parentId = 3L,
                            ),
                            BandalartCellEntity(
                                id = 19L,
                                title = null,
                                description = null,
                                isCompleted = false,
                                parentId = 3L,
                            ),
                        ),
                ),
                BandalartCellEntity(
                    id = 4L,
                    title = "넥나잇 준비",
                    description = null,
                    isCompleted = false,
                    parentId = 0L,
                    children =
                        listOf(
                            BandalartCellEntity(
                                id = 20L,
                                title = "세면도구 챙기기",
                                description = null,
                                isCompleted = true,
                                parentId = 4L,
                            ),
                            BandalartCellEntity(
                                id = 21L,
                                title = "라꾸라꾸 구매",
                                description = null,
                                isCompleted = false,
                                parentId = 4L,
                            ),
                            BandalartCellEntity(
                                id = 22L,
                                title = "노트북 충전기 챙기기",
                                description = null,
                                isCompleted = true,
                                parentId = 4L,
                            ),
                            BandalartCellEntity(
                                id = 23L,
                                title = "담뇨 챙기기",
                                description = null,
                                isCompleted = false,
                                parentId = 4L,
                            ),
                            BandalartCellEntity(
                                id = 24L,
                                title = "커피 준비",
                                description = null,
                                isCompleted = true,
                                parentId = 4L,
                            ),
                        ),
                ),
            ),
    )
