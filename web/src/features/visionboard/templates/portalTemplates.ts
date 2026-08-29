import type { VisionBoardTemplate } from '../visionBoardTemplates'

import imgCalmaBienestar01 from './assets/calma-bienestar-01.png'
import imgCalmaBienestar02 from './assets/calma-bienestar-02.png'
import imgCalmaBienestar03 from './assets/calma-bienestar-03.jpg'
import imgCalmaBienestar04 from './assets/calma-bienestar-04.jpg'
import imgEnergiaProgreso01 from './assets/energia-progreso-01.png'
import imgEnergiaProgreso02 from './assets/energia-progreso-02.jpg'
import imgEnergiaProgreso03 from './assets/energia-progreso-03.jpg'
import imgEnergiaProgreso04 from './assets/energia-progreso-04.jpg'
import imgEnergiaProgreso05 from './assets/energia-progreso-05.jpg'
import imgEnergiaProgreso06 from './assets/energia-progreso-06.jpg'
import imgEnergiaProgreso07 from './assets/energia-progreso-07.jpg'
import imgEnergiaProgreso08 from './assets/energia-progreso-08.jpg'
import imgEnergiaProgreso09 from './assets/energia-progreso-09.jpg'
import imgEnergiaProgreso10 from './assets/energia-progreso-10.jpg'
import imgEnergiaProgreso11 from './assets/energia-progreso-11.jpg'
import imgEnergiaProgreso12 from './assets/energia-progreso-12.jpg'
import imgEnergiaProgreso13 from './assets/energia-progreso-13.jpg'
import imgEnergiaProgreso14 from './assets/energia-progreso-14.jpg'
import imgEnergiaProgreso15 from './assets/energia-progreso-15.jpg'
import imgEnergiaProgreso16 from './assets/energia-progreso-16.jpg'
import imgEnergiaProgreso17 from './assets/energia-progreso-17.jpg'
import imgEnergiaProgreso18 from './assets/energia-progreso-18.jpg'
import imgLujoAspiraciones01 from './assets/lujo-aspiraciones-01.jpg'
import imgLujoAspiraciones02 from './assets/lujo-aspiraciones-02.jpg'
import imgLujoAspiraciones03 from './assets/lujo-aspiraciones-03.jpg'
import imgLujoAspiraciones04 from './assets/lujo-aspiraciones-04.png'
import imgLujoAspiraciones05 from './assets/lujo-aspiraciones-05.jpg'
import imgLujoAspiraciones06 from './assets/lujo-aspiraciones-06.jpg'
import imgLujoAspiraciones07 from './assets/lujo-aspiraciones-07.jpg'
import imgLujoAspiraciones08 from './assets/lujo-aspiraciones-08.jpg'
import imgLujoAspiraciones09 from './assets/lujo-aspiraciones-09.jpg'
import imgLujoAspiraciones10 from './assets/lujo-aspiraciones-10.jpg'
import imgLujoAspiraciones11 from './assets/lujo-aspiraciones-11.jpg'
import imgLujoAspiraciones12 from './assets/lujo-aspiraciones-12.png'
import imgLujoAspiraciones13 from './assets/lujo-aspiraciones-13.jpg'
import imgLujoAspiraciones14 from './assets/lujo-aspiraciones-14.jpg'
import imgLujoAspiraciones15 from './assets/lujo-aspiraciones-15.jpg'
import imgLujoAspiraciones16 from './assets/lujo-aspiraciones-16.jpg'
import imgLujoAspiraciones17 from './assets/lujo-aspiraciones-17.jpg'
import imgLujoAspiraciones18 from './assets/lujo-aspiraciones-18.jpg'
import imgLujoAspiraciones19 from './assets/lujo-aspiraciones-19.jpg'
import imgMetasConquistas01 from './assets/metas-conquistas-01.jpg'
import imgMetasConquistas02 from './assets/metas-conquistas-02.jpg'
import imgMetasConquistas03 from './assets/metas-conquistas-03.png'

/**
 * PLANTILLAS PERSONALIZADAS DEL PORTAL (2026-08-29, pedido explícito del
 * usuario).
 *
 * No son ejemplos ni maquetas: son cuatro Vision Boards que el usuario
 * diseñó en el propio portal y que quiere conservar y ofrecer como
 * plantillas reales. Este archivo es una **exportación fiel** de esos
 * tableros: cada elemento conserva su tipo, posición, tamaño, rotación,
 * orden de pintado y su `data` completo (incluidos `frameStyle`, textos y
 * pegatinas). No se rediseñó ni se simplificó nada.
 *
 * POR QUÉ LAS IMÁGENES SON ASSETS Y NO `imageId`:
 * en la base de datos cada IMAGE apunta a un `imageId` de
 * `vision_board_images`, y ese endpoint es **dueño-únicamente**
 * (`VisionBoardImageController.get` → `getOwnedOrThrow`). Una plantilla que
 * conservara esos ids se vería rota para cualquier otro usuario, y moriría
 * el día que se borrara el tablero original o su imagen. Como estas son
 * plantillas "que quiero conservar", las 44 imágenes se extrajeron
 * **byte a byte** de la base de datos a `./assets` y cada elemento las
 * referencia por `data.url`, que es una vía que el resolutor de imágenes ya
 * soportaba de antes (`useVisionBoardImageSrc`, rama `data.url`) — no hubo
 * que tocar el renderizador. Así la plantilla es autocontenida y
 * permanente.
 *
 * Los tableros de origen se quedan donde están, intactos.
 *
 * Las coordenadas no necesitaron conversión: los cuatro tableros miden
 * 1600x1000, exactamente el lienzo de referencia de las plantillas
 * (TEMPLATE_REFERENCE_WIDTH/HEIGHT).
 */
export const PORTAL_VISION_BOARD_TEMPLATES: VisionBoardTemplate[] = [
  {
    id: 'portal-lujo-aspiraciones',
    kind: 'portal',
    name: 'Lujo & Aspiraciones',
    description: 'Una visión sofisticada enfocada en estilo de vida, bienestar, logros y aspiraciones personales.',
    /** Origen: tablero "nuevo plantilla caso 1" del portal (22 elementos, tema LIGHT). */
    sourceBoardName: 'nuevo plantilla caso 1',
    theme: 'LIGHT',
    elements: [
      { type: 'IMAGE', x: 460, y: 48.57, width: 525.56, height: 848.73, data: { url: imgLujoAspiraciones01, frameStyle: "rounded" } },
      { type: 'IMAGE', x: 1402.03, y: 501.63, width: 197.97, height: 262.72, data: { url: imgLujoAspiraciones02 } },
      { type: 'IMAGE', x: 0, y: -32.06, width: 258.69, height: 356.37, data: { url: imgLujoAspiraciones03, frameStyle: "circle" } },
      { type: 'IMAGE', x: 460, y: 454.17, width: 175, height: 260, data: { url: imgLujoAspiraciones04 } },
      { type: 'IMAGE', x: 1257.46, y: 10.65, width: 342.54, height: 524.05, data: { url: imgLujoAspiraciones05 } },
      { type: 'IMAGE', x: 1225.27, y: 534.7, width: 279.24, height: 465.3, data: { url: imgLujoAspiraciones06, frameStyle: "rounded" } },
      { type: 'STICKER', x: 207.69, y: 8.57, width: 80, height: 80, data: { stickerId: "emotion" } },
      { type: 'STICKER', x: 386.69, y: 147.94, width: 80, height: 80, data: { stickerId: "coffee" } },
      { type: 'IMAGE', x: 0, y: 246.13, width: 518.51, height: 753.87, data: { url: imgLujoAspiraciones07 } },
      { type: 'IMAGE', x: 258.69, y: 292.49, width: 180, height: 260, data: { url: imgLujoAspiraciones08 } },
      { type: 'IMAGE', x: 992.25, y: 32.06, width: 266.51, height: 455.88, data: { url: imgLujoAspiraciones09 } },
      { type: 'IMAGE', x: 992.25, y: 623.07, width: 259.66, height: 375.66, rotation: -4.13, data: { url: imgLujoAspiraciones10, frameStyle: "polaroid" } },
      { type: 'IMAGE', x: 264.13, y: -19.84, width: 195.87, height: 312.33, rotation: -1.2, data: { url: imgLujoAspiraciones11, frameStyle: "film" } },
      { type: 'IMAGE', x: 754.27, y: 584.17, width: 293.01, height: 427.72, rotation: -0.11, data: { url: imgLujoAspiraciones12 } },
      { type: 'IMAGE', x: 1008.18, y: 415.93, width: 250.59, height: 348.41, rotation: 1.41, data: { url: imgLujoAspiraciones13, frameStyle: "rounded" } },
      { type: 'IMAGE', x: 756.09, y: 0, width: 226.1, height: 350.05, data: { url: imgLujoAspiraciones14 } },
      { type: 'IMAGE', x: 386.69, y: 415.93, width: 213.74, height: 357.94, data: { url: imgLujoAspiraciones15 } },
      { type: 'TEXT', x: 817.87, y: 292.49, width: 206.48, height: 63.64, data: { text: "Vision Board" } },
      { type: 'IMAGE', x: 515.73, y: 714.17, width: 238.54, height: 292.55, data: { url: imgLujoAspiraciones16 } },
      { type: 'IMAGE', x: 13, y: 204.55, width: 232.69, height: 310.88, rotation: -9.25, data: { url: imgLujoAspiraciones17 } },
      { type: 'IMAGE', x: 118.7, y: 566.34, width: 229.99, height: 396.01, data: { url: imgLujoAspiraciones18 } },
      { type: 'IMAGE', x: 600.43, y: 356.13, width: 233.68, height: 326, data: { url: imgLujoAspiraciones19 } },
    ],
  },
  {
    id: 'portal-calma-bienestar',
    kind: 'portal',
    name: 'Calma & Bienestar',
    description: 'Una visión de confort, tranquilidad, equilibrio y bienestar personal.',
    /** Origen: tablero "plantilla 2" del portal (5 elementos, tema PAPER). */
    sourceBoardName: 'plantilla 2',
    theme: 'PAPER',
    elements: [
      { type: 'IMAGE', x: 369.58, y: 277.38, width: 989.42, height: 445.24, rotation: 89.31, data: { url: imgCalmaBienestar01 } },
      { type: 'IMAGE', x: 0, y: 0, width: 260, height: 117, data: { url: imgCalmaBienestar02 } },
      { type: 'IMAGE', x: 0, y: 0, width: 739.16, height: 1000, data: { url: imgCalmaBienestar03 } },
      { type: 'IMAGE', x: 950.12, y: 0, width: 649.88, height: 1000, data: { url: imgCalmaBienestar04 } },
      { type: 'TEXT', x: 737.84, y: 351.98, width: 212.28, height: 148.02, data: { text: "2027" } },
    ],
  },
  {
    id: 'portal-metas-conquistas',
    kind: 'portal',
    name: 'Metas & Conquistas',
    description: 'Una visión enfocada en trabajo, viajes, vehículos, crecimiento y objetivos personales.',
    /** Origen: tablero "visioin board hombre" del portal (3 elementos, tema ENERGY). */
    sourceBoardName: 'visioin board hombre',
    theme: 'ENERGY',
    elements: [
      { type: 'IMAGE', x: 1027.2, y: 0, width: 572.8, height: 1000, data: { url: imgMetasConquistas01 } },
      { type: 'IMAGE', x: 0, y: 0, width: 581.37, height: 1000, data: { url: imgMetasConquistas02 } },
      { type: 'IMAGE', x: 581.37, y: 0, width: 445.83, height: 1000, data: { url: imgMetasConquistas03 } },
    ],
  },
  {
    id: 'portal-energia-progreso',
    kind: 'portal',
    name: 'Energía & Progreso',
    description: 'Una visión enfocada en viajes, negocios, ejercicio, disciplina y crecimiento personal.',
    /** Origen: tablero "vision board hombre 2" del portal (18 elementos, tema LIGHT). */
    sourceBoardName: 'vision board hombre 2',
    theme: 'LIGHT',
    elements: [
      { type: 'IMAGE', x: 0, y: -9.08, width: 1600, height: 724.23, data: { url: imgEnergiaProgreso01 } },
      { type: 'IMAGE', x: 327.51, y: -9.08, width: 289.38, height: 538.16, data: { url: imgEnergiaProgreso02 } },
      { type: 'IMAGE', x: 301, y: 529.08, width: 313, height: 470.92, data: { url: imgEnergiaProgreso03 } },
      { type: 'IMAGE', x: 55.2, y: 326.18, width: 272.31, height: 450.29, data: { url: imgEnergiaProgreso04 } },
      { type: 'IMAGE', x: 0, y: 0, width: 250.51, height: 376.12, data: { url: imgEnergiaProgreso05 } },
      { type: 'IMAGE', x: 1064.62, y: 405.69, width: 318.13, height: 388, data: { url: imgEnergiaProgreso06 } },
      { type: 'IMAGE', x: 616.89, y: 26.43, width: 296.16, height: 502.65, data: { url: imgEnergiaProgreso07 } },
      { type: 'IMAGE', x: 1334.61, y: 605.71, width: 265.39, height: 394.29, data: { url: imgEnergiaProgreso08 } },
      { type: 'IMAGE', x: 1382.75, y: 291.32, width: 151, height: 260, data: { url: imgEnergiaProgreso09 } },
      { type: 'IMAGE', x: 1316.5, y: 0, width: 283.5, height: 313.87, data: { url: imgEnergiaProgreso10 } },
      { type: 'IMAGE', x: 173.51, y: 740, width: 154, height: 260, data: { url: imgEnergiaProgreso11 } },
      { type: 'IMAGE', x: 552.22, y: 376.12, width: 239.03, height: 388.43, data: { url: imgEnergiaProgreso12 } },
      { type: 'IMAGE', x: 791.25, y: 567.04, width: 224.4, height: 395, data: { url: imgEnergiaProgreso13 } },
      { type: 'IMAGE', x: 606.6, y: 740, width: 234, height: 260, data: { url: imgEnergiaProgreso14 } },
      { type: 'IMAGE', x: 1003.61, y: 0, width: 281.23, height: 353.03, data: { url: imgEnergiaProgreso15 } },
      { type: 'IMAGE', x: 893.65, y: 227.32, width: 244, height: 356.74, data: { url: imgEnergiaProgreso16 } },
      { type: 'IMAGE', x: 0, y: 638.35, width: 191.35, height: 364.65, data: { url: imgEnergiaProgreso17 } },
      { type: 'IMAGE', x: 985.16, y: 671.68, width: 238.9, height: 328.32, data: { url: imgEnergiaProgreso18 } },
    ],
  },
]
