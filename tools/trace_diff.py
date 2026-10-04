"""集成测试轨迹二进制解码与对比工具。

用法:
  python tools/trace_diff.py <vanilla.bin> <optimized.bin> [max-frames]

轨迹格式(见 integrationTest 的 ZombieTrace/TntCrowd 写入端):
  header: magic i32, version i32, entity_count i32, ticks i32
  frame:  tick i32, [TNT: fuse i32, pos 3f64, delta 3f64, bounds 6f64,
          tickCount i32, removed u8, onGround u8](仅 TNT 场景),
          N × zombie( idx i32, pos 3f64, delta 3f64, bounds 6f64,
          xo..zOld 6f64, health f32, absorption f32, fallDistance f64,
          yRot/xRot/yHeadRot/yBodyRot/yRotO/xRotO/yHeadRotO/yBodyRotO 8×f32,
          tickCount/hurtTime/hurtDuration/deathTime/invulnerableTime/
          airSupply/remainingFire/frozen/noAction 9×i32, pose i32, flags i32 )
  tail:   (N+1) × random i64
"""
import struct
import sys

ZOMBIE_SIZE = 4 + 24 + 24 + 48 + 48 + 4 + 4 + 8 + 32 + 36 + 4 + 4
HAS_TNT = True


def parse(path, count, has_tnt=HAS_TNT):
    data = open(path, 'rb').read()
    off = 16
    end = len(data) - 8 * (count + 1)
    frames = []
    tnt_size = (4 + 24 + 24 + 48 + 4 + 1 + 1) if has_tnt else 0
    while off < end:
        tick = struct.unpack_from('>i', data, off)[0]
        off += 4 + tnt_size
        zs = []
        for _ in range(count):
            off += 4 + 24 + 24 + 48 + 48 + 4 + 4 + 8 + 32
            ints = struct.unpack_from('>9i', data, off)
            off += 36 + 4 + 4
            zs.append(ints)
        frames.append((tick, zs))
    assert off == end, (off, end)
    return frames


def compare(a_path, b_path, max_frames=8):
    a = open(a_path, 'rb').read()
    b = open(b_path, 'rb').read()
    if len(a) != len(b):
        print(f"长度不同: {len(a)} vs {len(b)}")
    first = next((i for i in range(min(len(a), len(b))) if a[i] != b[i]), None)
    print("首差异字节:", first if first is not None else "无(逐字节一致)")
    count = struct.unpack_from('>i', a, 8)[0]
    fa, fb = parse(a_path, count), parse(b_path, count)
    for fi in range(min(max_frames, len(fa), len(fb))):
        ta, za = fa[fi]
        tb, zb = fb[fi]
        print(f"frame {fi} tick={ta}: tickCount van={sorted(set(z[0] for z in za))} "
              f"opt={sorted(set(z[0] for z in zb))} "
              f"hurt van={sorted(set(z[1] for z in za))} opt={sorted(set(z[1] for z in zb))}")


if __name__ == '__main__':
    compare(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 8)
